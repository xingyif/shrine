package net.shrine.qep

import net.shrine.aggregation.{Aggregator, Aggregators, DeleteQueryAggregator, FlagQueryAggregator, ReadInstanceResultsAggregator, ReadQueryDefinitionAggregator, RenameQueryAggregator, RunQueryAggregator, UnFlagQueryAggregator}
import net.shrine.audit.NetworkQueryId
import net.shrine.authentication.AuthenticationResult.Authenticated
import net.shrine.authentication.{AuthenticationResult, Authenticator, NotAuthenticatedException}
import net.shrine.authorization.AuthorizationResult.{Authorized, NotAuthorized}
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.BroadcastAndAggregationService
import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.{AggregatedReadInstanceResultsResponse, AggregatedRunQueryResponse, AuthenticationInfo, BaseShrineRequest, BaseShrineResponse, Credential, DeleteQueryRequest, FlagQueryRequest, NodeId, QueryInstance, QueryResult, ReadApprovedQueryTopicsRequest, ReadInstanceResultsRequest, ReadPreviousQueriesRequest, ReadPreviousQueriesResponse, ReadQueryDefinitionRequest, ReadQueryInstancesRequest, ReadQueryInstancesResponse, ReadResultOutputTypesRequest, ReadResultOutputTypesResponse, RenameQueryRequest, ResultOutputType, RunQueryRequest, UnFlagQueryRequest}
import net.shrine.qep.audit.QepAuditDb
import net.shrine.qep.dao.AuditDao
import net.shrine.qep.querydb.{QepQuery, QepQueryDb}
import net.shrine.util.XmlDateHelper

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal
import scala.xml.NodeSeq

/**
 * @author clint
 * @since Feb 19, 2014
 */

trait AbstractQepService[BaseResp <: BaseShrineResponse] extends Loggable {
  val commonName:String
  val auditDao: AuditDao
  val authenticator: Authenticator
  val authorizationService: QueryAuthorizationService
  val includeAggregateResult: Boolean
  val broadcastAndAggregationService: BroadcastAndAggregationService
  val queryTimeout: Duration
  val breakdownTypes: Set[ResultOutputType]
  val collectQepAudit:Boolean
  val nodeId:NodeId

  protected def doReadResultOutputTypes(request: ReadResultOutputTypesRequest): BaseResp = {
    info(s"doReadResultOutputTypes($request)")

    authenticateAndThen(request) { authResult =>
      val resultOutputTypes = ResultOutputType.nonErrorTypes ++ breakdownTypes

      //TODO: XXX: HACK: Would like to remove the cast
      ReadResultOutputTypesResponse(resultOutputTypes).asInstanceOf[BaseResp]
    }
  }
  
  protected def doFlagQuery(request: FlagQueryRequest, shouldBroadcast: Boolean = true): BaseResp = {
    authenticateAndThen(request) { authResult =>
      QepQueryDb.db.insertQepQueryFlag(request)
      doBroadcastQuery(request, new FlagQueryAggregator, shouldBroadcast,authResult)
    }
  }
  
  protected def doUnFlagQuery(request: UnFlagQueryRequest, shouldBroadcast: Boolean = true): BaseResp = {
    authenticateAndThen(request) { authResult =>
      QepQueryDb.db.insertQepQueryFlag(request)
      doBroadcastQuery(request, new UnFlagQueryAggregator, shouldBroadcast,authResult)
    }
  }
  
  protected def doRunQuery(request: RunQueryRequest, shouldBroadcast: Boolean): BaseResp = {
    authenticateAndThen(request) { authResult =>
      info(s"doRunQuery($request,$shouldBroadcast) with $runQueryAggregatorFor")

      //store the query in the qep's database

      doBroadcastQuery(request, runQueryAggregatorFor(request), shouldBroadcast,authResult)
    }
  }

  protected def doReadQueryDefinition(request: ReadQueryDefinitionRequest, shouldBroadcast: Boolean): BaseResp = {
    authenticateAndThen(request) { authResult =>
      info(s"doReadQueryDefinition($request,$shouldBroadcast)")

      doBroadcastQuery(request, new ReadQueryDefinitionAggregator, shouldBroadcast,authResult)
    }
  }

  protected def doReadInstanceResults(request: ReadInstanceResultsRequest, shouldBroadcast: Boolean): BaseResp = {
    authenticateAndThen(request) { authResult =>
      info(s"doReadInstanceResults($request,$shouldBroadcast)")

      val networkId = request.shrineNetworkQueryId

      //read from the QEP database code here. Only broadcast if some result is in some sketchy state
      val queryFromDb: Option[QepQuery] = QepQueryDb.db.selectQueryById(networkId)
      val resultsFromDb: Seq[QueryResult] = QepQueryDb.db.selectMostRecentQepResultsFor(networkId)

      debug(s"result states are ${resultsFromDb.map(_.statusType).mkString(" ")}")

      def shouldAskAdapters:Boolean = {
        queryFromDb.fold(
          true //the QEP doesn't know about the query. Maybe the adapter will know
        ){ q:QepQuery =>

          //todo remove this ball of logic (and most of this class) when all communication between the QEP and Hub is via messaging
          val now = System.currentTimeMillis()
          val deadline = q.dateCreated + queryTimeout.toMillis
          if (now > deadline) resultsFromDb.isEmpty || resultsFromDb.exists(!_.statusType.isDone) //If the QEP should have given up waiting on the hub and some result is not done
          else if (resultsFromDb.isEmpty) false //Don't ask if no results exist. There's still time
          else
            resultsFromDb.forall(_.statusType.isCrcCallCompleted) && //Be sure every CRC has replied to its adapter
              resultsFromDb.exists(_.statusType.crcPromisedToFinishAfterReply) //And that some CRC said to ask later
        }
      }

      val response = if (!shouldAskAdapters) {
        debug(s"Using qep cached results for query $networkId")
        AggregatedReadInstanceResultsResponse(networkId, resultsFromDb).asInstanceOf[BaseResp]
      }
      else {
        info(s"Requesting results for $networkId from network")
        val response = doBroadcastQuery(request, new ReadInstanceResultsAggregator(networkId, false), shouldBroadcast,authResult)

        //put the new results in the database if we got what we wanted
        response match {
          case arirr: AggregatedReadInstanceResultsResponse => arirr.results.foreach(r => QepQueryDb.db.insertQueryResult(networkId, r))
          case _ => warn(s"Response was a ${response.getClass.getSimpleName}, not a ${classOf[AggregatedReadInstanceResultsResponse].getSimpleName}: $response")
        }
        response
      }
      response
    }
  }

  protected def doReadQueryInstances(request: ReadQueryInstancesRequest, shouldBroadcast: Boolean): BaseResp = {
    authenticateAndThen(request) { authResult =>
      info(s"doReadQueryInstances($request,$shouldBroadcast)")
      val now = XmlDateHelper.now
      val networkQueryId = request.networkQueryId
      val username = request.authn.username
      val groupId = request.projectId

      //NB: Return a dummy response, with a dummy QueryInstance containing the network (Shrine) id of the query we'd like
      //to get "instances" for.  This allows the legacy web client to formulate a request for query results that Shrine
      //can understand, while meeting the conversational requirements of the legacy web client.
      val instance = QueryInstance(networkQueryId.toString, networkQueryId.toString, username, groupId, now, now)

      //TODO: XXX: HACK: Would like to remove the cast
      //NB: Munge in username from authentication result
      ReadQueryInstancesResponse(networkQueryId, authResult.username, groupId, Seq(instance)).asInstanceOf[BaseResp]
    }
  }

  protected def doReadPreviousQueries(request: ReadPreviousQueriesRequest, shouldBroadcast: Boolean): ReadPreviousQueriesResponse = {
    authenticateAndThen(request){ authResult =>
      info(s"doReadPreviousQueries($request,$shouldBroadcast)")

      //todo if any results are in one of the pending states go ahead and request them async (has to wait for async Shrine 1.24)
      //pull queries from the local database.
      QepQueryDb.db.selectPreviousQueries(request)
    }
  }

  protected def doRenameQuery(request: RenameQueryRequest, shouldBroadcast: Boolean): BaseResp = {
    authenticateAndThen(request) { authResult =>
      info(s"doRenameQuery($request,$shouldBroadcast)")
      QepQueryDb.db.renamePreviousQuery(request)
      doBroadcastQuery(request, new RenameQueryAggregator, shouldBroadcast,authResult)
    }
  }

  protected def doDeleteQuery(request: DeleteQueryRequest, shouldBroadcast: Boolean): BaseResp = {
    authenticateAndThen(request) { authResult =>
      info(s"doDeleteQuery($request,$shouldBroadcast)")
      QepQueryDb.db.markDeleted(request)
      doBroadcastQuery(request, new DeleteQueryAggregator, shouldBroadcast,authResult)
    }
  }

  protected def doReadApprovedQueryTopics(request: ReadApprovedQueryTopicsRequest, shouldBroadcast: Boolean): BaseResp = authenticateAndThen(request) { _ =>
    info(s"doReadApprovedQueryTopics($request,$shouldBroadcast)")
    //TODO: XXX: HACK: Would like to remove the cast
    authorizationService.readApprovedEntries(request) match {
      case Left(errorResponse) => errorResponse.asInstanceOf[BaseResp]
      case Right(validResponse) => validResponse.asInstanceOf[BaseResp]
    }
  }

  import broadcastAndAggregationService.sendAndAggregate

  protected def doBroadcastQuery(request: BaseShrineRequest, aggregator: Aggregator, shouldBroadcast: Boolean, authResult:Authenticated): BaseResp = {

    debug(s"doBroadcastQuery($request) authResult is $authResult")
    //NB: Use credentials obtained from Authenticator (oddly, we authenticate with one set of credentials and are "logged in" under (possibly!) another
    //NB: Only audit RunQueryRequests

    //When making BroadcastMessages
    val networkAuthn = AuthenticationInfo(authResult.domain, authResult.username, Credential("", isToken = false))

    /** Sends the query to the hub and starts a Future to watch for the results. Returns (almost) immediately. */
    def queryHub( authorizedRequest: RunQueryRequest): Unit = {
      import scala.concurrent.ExecutionContext.Implicits.global
      import scala.concurrent.blocking

      info(s"Sending RunQueryRequest ${authorizedRequest.networkQueryId} to the Hub")
      //Future[Unit] that this code fires off to the hub, then handles the BaseShrineResponse
      sendAndAggregate(networkAuthn,authorizedRequest,aggregator,shouldBroadcast).transform ( { hubResponse:BaseShrineResponse =>
        debug(s"Received $hubResponse for ${authorizedRequest.networkQueryId}")
          hubResponse match {
          case aggregated: AggregatedRunQueryResponse =>
            info(s"Received ${aggregated.statusTypeName} and ignored results for ${authorizedRequest.networkQueryId}")
            blocking {
              //now that queries arrive at the QEP via a queue, no need to put them into the database. They can just fall on the floor
              //todo record the query's state in a way that will stop polling in 1.23. See SHRINE-2148
            }
          case _ => IncorrectResponseFromHub(hubResponse,authorizedRequest.networkQueryId)
          }
        }
      , throwable => {
          throwable match {
            case NonFatal(t) => ExceptionWhileHubRanQuery(t, authorizedRequest.networkQueryId)
            case _ => //Let the infrastructure handle fatal exceptions
          }
          throwable
        }
      )
    }

    request match {
      case runQueryRequest: RunQueryRequest =>
        // inject modified, authorized runQueryRequest
//although it might make more sense to put this whole if block in the aggregator, the RunQueryAggregator lives in the hub, far from this DB code
        //inject QEP NodeId
        auditAuthorizeAndThen(runQueryRequest.copy(nodeId = Some(nodeId))) { authorizedRequest =>
          debug(s"doBroadcastQuery authorizedRequest is $authorizedRequest")

          // tuck the ACT audit metrics data into a database here
          if (collectQepAudit) QepAuditDb.db.insertQepQuery(authorizedRequest,commonName)
          QepQueryDb.db.insertQepQuery(authorizedRequest)

          queryHub(authorizedRequest)

          val response = AggregatedRunQueryResponse(
            queryId = authorizedRequest.networkQueryId,
            createDate = XmlDateHelper.now,
            userId = networkAuthn.username,
            groupId = networkAuthn.domain,
            requestXml = authorizedRequest.queryDefinition,
            queryInstanceId = authorizedRequest.networkQueryId,
            results = Seq.empty,
            statusTypeName = "RECEIVED_BY_QEP" //todo figure out the right statuses for 1.23. See SHRINE-2148
          )

          response.asInstanceOf[BaseResp]
        }
      case _ => doSynchronousQuery(networkAuthn,request,aggregator,shouldBroadcast)
    }
  }

  private def doSynchronousQuery(networkAuthn: AuthenticationInfo,request: BaseShrineRequest, aggregator: Aggregator, shouldBroadcast: Boolean) = {
    info(s"doSynchronousQuery($request) started")
    val response = waitFor(sendAndAggregate(networkAuthn, request, aggregator, shouldBroadcast)).asInstanceOf[BaseResp]
    info(s"doSynchronousQuery($request) completed with response $response")
    response
  }

  private[qep] val runQueryAggregatorFor: RunQueryRequest => RunQueryAggregator = Aggregators.forRunQueryRequest(includeAggregateResult)

  protected def waitFor[R](futureResponse: Future[R]): R = {
    XmlDateHelper.time("Waiting for aggregated results")(debug(_)) {
      Await.result(futureResponse, queryTimeout)
    }
  }

  private[qep] def auditAuthorizeAndThen[T](request: RunQueryRequest)(body: (RunQueryRequest => T)): T = {
    auditTransactionally(request) {

      debug(s"auditAuthorizeAndThen($request) with $authorizationService")

      val authorizedRequest = authorizationService.authorizeRunQueryRequest(request) match {
        case na: NotAuthorized => throw na.toException
        case authorized: Authorized => request.copy(topicName = authorized.topicIdAndName.map(x => x._2))
      }

      body(authorizedRequest)
    }
  }

  private[qep] def auditTransactionally[T](request: RunQueryRequest)(body: => T): T = {
    try { body } finally {
      auditDao.addAuditEntry(
        request.projectId,
        request.authn.domain,
        request.authn.username,
        request.queryDefinition.toI2b2String, //TODO: Use i2b2 format Still?
        request.topicId)
    }
  }

  //todo move auth code with SHRINE-1322
  import AuthenticationResult._
  
  private[qep] def authenticateAndThen[T](request: BaseShrineRequest)(f: Authenticated => T): T = {
    val authResult = authenticator.authenticate(request.authn)

    authResult match {
      case a: Authenticated => f(a)
      case na:NotAuthenticated => throw NotAuthenticatedException(na)
    }
  }
}

case class ExceptionWhileHubRanQuery(t: Throwable,networkQueryId: NetworkQueryId) extends AbstractProblem(ProblemSources.Qep) {

  override val throwable = Some(t)

  override def summary: String = s"${t.getClass.getSimpleName} encountered in an http call to run $networkQueryId query at the hub."

  override def description: String = "The QEP generated an exception while making an http call to run a query at the hub."

  override def detailsXml: NodeSeq = NodeSeq.fromSeq(<details>
    networkQueryId is {networkQueryId}
    {throwableDetail.getOrElse("")}
  </details>)

}

case class IncorrectResponseFromHub(hubResponse:BaseShrineResponse,networkQueryId: NetworkQueryId) extends AbstractProblem(ProblemSources.Qep) {

  override def summary: String = s"The hub responded to query $networkQueryId with a ${hubResponse.getClass.getSimpleName}, not a ${classOf[AggregatedRunQueryResponse].getSimpleName}"

  override def description: String = s"The hub responded with something other than a ${classOf[AggregatedRunQueryResponse].getSimpleName} to the QEP's run query request."

  override def detailsXml: NodeSeq = NodeSeq.fromSeq(
    <details>
      networkQueryId is {networkQueryId}
      hubResponse is {hubResponse}
    </details>)
}