package net.shrine.adapter

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import net.shrine.adapter.audit.AdapterAuditDb

import scala.Option.option2Iterable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.model.{Breakdown, ShrineQuery, ShrineQueryResult}
import net.shrine.protocol.{AuthenticationInfo, BaseShrineRequest, BroadcastMessage, ErrorResponse, HasQueryResults, HiveCredentials, QueryResult, ReadQueryInstancesRequest, ReadQueryInstancesResponse, ReadResultRequest, ReadResultResponse, ResultOutputType, ShrineRequest, ShrineResponse}
import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.Tries.sequence

import scala.concurrent.duration.Duration
import net.shrine.client.{HttpResponse, Poster}
import net.shrine.problem.{AbstractProblem, ProblemSources}

/**
 * @author clint
 * @since Nov 2, 2012
 *
 */
object AbstractReadQueryResultAdapter {
  private final case class RawResponseAttempts(countResponseAttempt: Try[ReadResultResponse], breakdownResponseAttempts: Seq[Try[ReadResultResponse]])

  private final case class SpecificResponseAttempts[R](responseAttempt: Try[R], breakdownResponseAttempts: Seq[Try[ReadResultResponse]])
}

//noinspection RedundantBlock
abstract class AbstractReadQueryResultAdapter[Req <: BaseShrineRequest, Rsp <: ShrineResponse with HasQueryResults](
  poster: Poster,
  override val hiveCredentials: HiveCredentials,
  dao: AdapterDao,
  doObfuscation: Boolean,
  getQueryId: Req => Long,
  getProjectId: Req => String,
  toResponse: (Long, QueryResult) => Rsp,
  breakdownTypes: Set[ResultOutputType],
  collectAdapterAudit:Boolean,
  obfuscator:Obfuscator
) extends WithHiveCredentialsAdapter(hiveCredentials) {

  //TODO: Make this configurable 
  private val numThreads = math.max(5, Runtime.getRuntime.availableProcessors)

  //TODO: Use scala.concurrent.ExecutionContext.Implicits.global instead?
  private lazy val executorService = Executors.newFixedThreadPool(numThreads)

  private lazy val executionContext = ExecutionContext.fromExecutorService(executorService)

  override def shutdown() {
    try {
      executorService.shutdown()

      executorService.awaitTermination(5, TimeUnit.SECONDS)
    } finally {
      executorService.shutdownNow()

      super.shutdown()
    }
  }

  import AbstractReadQueryResultAdapter._

  override protected[adapter] def processRequest(message: BroadcastMessage): ShrineResponse = {
    //Req is either  a ReadQueryResultRequest or a ReadInstanceResultsRequest. Both have the project id
    val req = message.request.asInstanceOf[Req]

    val queryId: Long = getQueryId(req)

    def maybeShrineQueryRow: Option[ShrineQuery] = dao.findQueryByNetworkId(queryId)

    def maybeShrineQueryResults: Option[ShrineQueryResult] = dao.findResultsFor(queryId)

    //todo a pair of getOrElse would be better than the match/case
    maybeShrineQueryRow match {
      case None => {
        debug(s"Query $queryId not found in the Shrine DB")

        ErrorResponse(QueryNotFound(queryId))
      }
      case Some(shrineQueryRow) => {

        maybeShrineQueryResults match {
          case None => {
            debug(s"Query $queryId found but its results are not available")

            //TODO: When precisely can this happen?  Should we go back to the CRC here?

            ErrorResponse(QueryResultNotAvailable(queryId))
          }
          case Some(shrineQueryResult) => {
            if (shrineQueryResult.isDone) {
              debug(s"Query $queryId is done and already stored, returning stored results")

              makeResponseFrom(queryId, shrineQueryResult)
            } else {
              debug(s"Query $queryId is incomplete, asking CRC for results")

              //Use a ReadQueryInstancesRequest to find out if it is safe to ask for the results
              val readQueryInstancesRequest = ReadQueryInstancesRequest(hiveCredentials.projectId,req.waitTime,hiveCredentials.toAuthenticationInfo,shrineQueryRow.localId.toLong)

              val httpResponse: HttpResponse = poster.post(readQueryInstancesRequest.toI2b2String)
              debug(s"ReadQueryInstancesResponse httpResponse for $queryId is $httpResponse")

              val tryResult = if(httpResponse.statusCode == 200){

                val maybeReadQueryInstanceResponse: Try[ReadQueryInstancesResponse] = Try{
                  ReadQueryInstancesResponse.fromI2b2(httpResponse.body)
                }
                debug(s"maybeReadQueryInstanceResponse.get is ${maybeReadQueryInstanceResponse.get}")

                maybeReadQueryInstanceResponse.transform({ rqiResponse =>
                  //todo get status into the response if(rqiResponse.)
                  //if any
                  if(rqiResponse.queryInstances.forall(_.queryStatus.isDone)) {
                    //todo start here. This is asking for results, which is not safe yet. First ask to see if the wait is over.
                    val result: ShrineResponse = retrieveQueryResults(queryId, req, shrineQueryResult, message)
                    if (collectAdapterAudit) AdapterAuditDb.db.insertResultSent(queryId,result)
                    Success(result)
                  } else {
                    //todo Failure(new IllegalStateException("Query is still in an incomplete state"))//todo send back "Still queued" response Success(rqiResponse)
                    debug(s"Query is still in an incomplete state. Try the experiment anyway. $rqiResponse ")

                    //todo try anyway for the current experiment
                    val result: ShrineResponse = retrieveQueryResults(queryId, req, shrineQueryResult, message)
                    if (collectAdapterAudit) AdapterAuditDb.db.insertResultSent(queryId,result)
                    Success(result)
                  }
                },{ x =>
                  Failure(x)
                })
              } else {
                //todo report problem before sending incomplete status again
                error(s"Got status code ${httpResponse.statusCode} from the CRC, expected 200 OK. $httpResponse")
                Failure(new IllegalStateException(s"Got status code ${httpResponse.statusCode} from the CRC, expected 200 OK. $httpResponse"))
              }
              tryResult.get
            }
          }
        }
      }
    }
  }

  private def makeResponseFrom(queryId: Long, shrineQueryResult: ShrineQueryResult): ShrineResponse = {
    shrineQueryResult.toQueryResults(doObfuscation).map(toResponse(queryId, _)).getOrElse(ErrorResponse(QueryNotFound(queryId)))
  }

  private def retrieveQueryResults(queryId: Long, req: Req, shrineQueryResult: ShrineQueryResult, message: BroadcastMessage): ShrineResponse = {
    //NB: If the requested query was not finished executing on the i2b2 side when Shrine recorded it, attempt to
    //retrieve it and all its sub-components (breakdown results, if any) in parallel.  Asking for the results in
    //parallel is quite possibly too clever, but may be faster than asking for them serially.
    //TODO: Review this.

    //Make requests for results in parallel
    val futureResponses = scatter(message.networkAuthn, req, shrineQueryResult)

    //Gather all the results (block until they're all returned)
    val SpecificResponseAttempts(countResponseAttempt, breakdownResponseAttempts) = gather(queryId, futureResponses, req.waitTime)

    countResponseAttempt match {
      //If we successfully received the parent response (the one with query type PATIENT_COUNT_XML), re-store it along
      //with any retrieved breakdowns before returning it.
      case Success(countResponse) => {
        //NB: Only store the result if needed, that is, if all results are done
        //TODO: REVIEW THIS
        storeResultIfNecessary(shrineQueryResult, countResponse, req.authn, queryId, getFailedBreakdownTypes(breakdownResponseAttempts))

        countResponse
      }
      case Failure(e) => ErrorResponse(CouldNotRetrieveQueryFromCrc(queryId,e))
    }
  }

  private def scatter(authn: AuthenticationInfo, req: Req, shrineQueryResult: ShrineQueryResult): Future[RawResponseAttempts] = {

    def makeRequest(localResultId: Long) = ReadResultRequest(hiveCredentials.projectId, req.waitTime, hiveCredentials.toAuthenticationInfo, localResultId.toString)

    def process(localResultId: Long): ShrineResponse = {
      delegateResultRetrievingAdapter.process(authn, makeRequest(localResultId))
    }

    implicit val executionContext = this.executionContext
    import scala.concurrent.blocking

    def futureBlockingAttempt[T](f: => T): Future[Try[T]] = Future(blocking(Try(f)))

    val futureCountAttempt: Future[Try[ShrineResponse]] = futureBlockingAttempt {
      process(shrineQueryResult.count.localId)
    }

    val futureBreakdownAttempts = Future.sequence(for {
      Breakdown(_, localResultId, resultType, data) <- shrineQueryResult.breakdowns
    } yield futureBlockingAttempt {
      process(localResultId)
    })

    //Log errors retrieving count
    futureCountAttempt.collect {
      case Success(e: ErrorResponse) => error(s"Error requesting count result from the CRC: '$e'")
      case Failure(e) => error(s"Error requesting count result from the CRC: ", e)
    }

    //Log errors retrieving breakdown
    for {
      breakdownResponseAttempts <- futureBreakdownAttempts
    } {
      breakdownResponseAttempts.collect {
        case Success(e: ErrorResponse) => error(s"Error requesting breakdown result from the CRC: '$e'")
        case Failure(e) => error(s"Error requesting breakdown result from the CRC: ", e)
      }
    }

    //"Filter" for non-ErrorResponses
    val futureNonErrorCountAttempt: Future[Try[ReadResultResponse]] = futureCountAttempt.collect {
      case Success(resp: ReadResultResponse) => Success(resp) //NB: Need to repackage response here to avoid ugly, obscure, superfluous cast
      case unexpected => Failure(new Exception(s"Getting count result failed. Response is: '$unexpected'"))
    }

    //"Filter" for non-ErrorResponses
    val futureNonErrorBreakdownResponseAttempts: Future[Seq[Try[ReadResultResponse]]] = for {
      breakdownResponseAttempts <- futureBreakdownAttempts
    } yield {
      breakdownResponseAttempts.collect {
        case Success(resp: ReadResultResponse) => Try(resp)
      }
    }

    for {
      countResponseAttempt <- futureNonErrorCountAttempt
      breakdownResponseAttempts <- futureNonErrorBreakdownResponseAttempts
    } yield {
      RawResponseAttempts(countResponseAttempt, breakdownResponseAttempts)
    }
  }

  private def gather(queryId: Long, futureResponses: Future[RawResponseAttempts], waitTime: Duration): SpecificResponseAttempts[Rsp] = {

    val RawResponseAttempts(countResponseAttempt, breakdownResponseAttempts) = Await.result(futureResponses, waitTime)

    //Log any failures
    (countResponseAttempt +: breakdownResponseAttempts).collect { case Failure(e) => e }.foreach(error("Error retrieving result from the CRC: ", _))

    //NB: Count response and ALL breakdown responses must be available (not Failures) or else a Failure will be returned
    val responseAttempt = for {
      countResponse: ReadResultResponse <- countResponseAttempt
      countQueryResult = countResponse.metadata
      
      breakdownResponses: Seq[ReadResultResponse] <- sequence(breakdownResponseAttempts)
    } yield {
      val breakdownsByType = (for {
        breakdownResponse <- breakdownResponses
        resultType <- breakdownResponse.metadata.resultType
      } yield resultType -> breakdownResponse.data).toMap

      val queryResultWithBreakdowns = countQueryResult.withBreakdowns(breakdownsByType)

      val queryResultToReturn = if(doObfuscation) obfuscator.obfuscate(queryResultWithBreakdowns) else queryResultWithBreakdowns
      
      toResponse(queryId, queryResultToReturn)
    }

    SpecificResponseAttempts(responseAttempt, breakdownResponseAttempts)
  }

  private def getFailedBreakdownTypes(attempts: Seq[Try[ReadResultResponse]]): Set[ResultOutputType] = {
    val successfulBreakdownTypes = attempts.collect { case Success(ReadResultResponse(_, metadata, _)) => metadata.resultType }.flatten

    breakdownTypes -- successfulBreakdownTypes
  }

  private def storeResultIfNecessary(shrineQueryResult: ShrineQueryResult, response: Rsp, authn: AuthenticationInfo, queryId: Long, failedBreakdownTypes: Set[ResultOutputType]) {
    val responseIsDone = response.results.forall(_.statusType.isDone)

    if (responseIsDone) {
      storeResult(shrineQueryResult, response, authn, queryId, failedBreakdownTypes)
    }
  }

  private def storeResult(shrineQueryResult: ShrineQueryResult, response: Rsp, authn: AuthenticationInfo, queryId: Long, failedBreakdownTypes: Set[ResultOutputType]) {
    val rawResults = response.results
    val obfuscatedResults = obfuscator.obfuscateResults(doObfuscation)(response.results)

    for {
      shrineQuery <- dao.findQueryByNetworkId(queryId)
      queryResult <- rawResults.headOption
      obfuscatedQueryResult <- obfuscatedResults.headOption
    } {
      val queryDefinition = QueryDefinition(shrineQuery.name, shrineQuery.queryDefinition.expr)

      dao.inTransaction {
        dao.deleteQuery(queryId)

        dao.storeResults(authn, shrineQueryResult.localId, queryId, queryDefinition, rawResults, obfuscatedResults, failedBreakdownTypes.toSeq, queryResult.breakdowns, obfuscatedQueryResult.breakdowns)
      }
    }
  }

  private type Unmarshaller[R] = Set[ResultOutputType] => NodeSeq => Try[R]
  
  private final class DelegateAdapter[Rqst <: ShrineRequest, Rspns <: ShrineResponse](unmarshaller: Unmarshaller[Rspns]) extends CrcAdapter[Rqst, Rspns](poster, hiveCredentials) {
    def process(authn: AuthenticationInfo, req: Rqst): Rspns = processRequest(BroadcastMessage(authn, req)).asInstanceOf[Rspns]

    override protected def parseShrineResponse(xml: NodeSeq): ShrineResponse = unmarshaller(breakdownTypes)(xml).get //TODO: Avoid .get call
  }

  private lazy val delegateResultRetrievingAdapter = new DelegateAdapter[ReadResultRequest, ReadResultResponse](ReadResultResponse.fromI2b2)
}

case class QueryNotFound(queryId:Long) extends AbstractProblem(ProblemSources.Adapter) {
  override def summary: String = s"Query not found"
  override def description:String = s"No query with id $queryId found on ${stamp.host.getHostName}"
}

case class QueryResultNotAvailable(queryId:Long) extends AbstractProblem(ProblemSources.Adapter) {
  override def summary: String = s"Query $queryId found but its results are not available yet"
  override def description:String = s"Query $queryId found but its results are not available yet on ${stamp.host.getHostName}"
}

case class CouldNotRetrieveQueryFromCrc(queryId:Long,x: Throwable) extends AbstractProblem(ProblemSources.Adapter) {
  override def summary: String = s"Could not retrieve query $queryId from the CRC"
  override def description:String = s"Unhandled exception while retrieving query $queryId while retrieving it from the CRC on ${stamp.host.getHostName}"
  override def throwable = Some(x)
}
