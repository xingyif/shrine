package net.shrine.metadata

import java.util.UUID

import akka.actor.ActorSystem
import net.shrine.audit.{NetworkQueryId, QueryName, Time}
import net.shrine.authorization.steward.UserName
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, ProblemDigest, ProblemSources}
import net.shrine.protocol.ResultOutputType
import net.shrine.qep.querydb.{FullQueryResult, QepQuery, QepQueryBreakdownResultsRow, QepQueryDb, QepQueryDbChangeNotifier, QepQueryFlag}
import net.shrine.source.ConfigSource
import rapture.json._
import rapture.json.formatters.humanReadable
import rapture.json.jsonBackends.jawn._
import spray.http.{StatusCode, StatusCodes}
import spray.routing._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Try
import scala.util.control.NonFatal

/**
  * An API to support the web client's work with queries.
  *
  * The current API supplies information about previous running queries. Eventually this will support accessing
  * information about queries running now and the ability to submit queries.
  */

//todo move this to the qep/service module, or somewhere in the qep subproject
trait QepService extends HttpService with Loggable {

  def system: ActorSystem
  val qepQueryDbChangeNotifier = QepQueryDbChangeNotifier(system)

  val qepInfo =
    """
      |The SHRINE query entry point service.
      |
      |This API gives a researcher access to queries, and (eventually) the ability to run queries.
      |
    """.stripMargin

  def qepRoute(user: User): Route = pathPrefix("qep") {
    get {
      detach(){
        queryResult(user) ~ queryResultsTable(user)
      }
    } ~
      pathEndOrSingleSlash{complete(qepInfo)} ~
      respondWithStatus(StatusCodes.NotFound){complete(qepInfo)}
  }

  /*
Races to complete are OK in spray. They're already happening, in fact.

When a request comes in

if the request can be fulfilled immediately then do that
if not
  create a promise to fulfil to trigger the complete
  create a promise to bump that first one on timeout
  schedule a runnable to bump the timeout promise

  create a promise to bump that first one if the conditions are right
  create a promise to bump the conditional one and stuff it in a concurrent map for other parts of the system to find

  onSuccess remove the conditional promise and cancel the scheduled timeout.
*/
  def queryResult(user:User):Route = path("queryResult" / LongNumber) { queryId: NetworkQueryId =>

    //take optional parameters for version and an awaitTime, but insist on both
    //If the timeout parameter isn't supplied then the deadline is now so it will reply immediately
    parameters('afterVersion.as[Long] ? 0L, 'timeoutSeconds.as[Long] ? 0L) { (afterVersion: Long, timeoutSeconds: Long) =>

      //check that the timeout is less than the spray "give up" timeout
      val sprayRequestTimeout = ConfigSource.config.getLong("spray.can.server.request-timeout")
      val maximumTimeout = sprayRequestTimeout - 1

      if (maximumTimeout <= timeoutSeconds) warn(s"""spray.can.server.request-timeout $sprayRequestTimeout is too short
           |relative to timeoutSeconds $timeoutSeconds . The server may produce a timeout-related error. Using
           |$maximumTimeout instead of $timeoutSeconds to try to prevent that.""".stripMargin)
      val timeout = Seq(maximumTimeout,timeoutSeconds).min

      //times for local races.
      val requestStartTime = System.currentTimeMillis()
      val deadline = requestStartTime + (timeout * 1000)

      detach(){
        val troubleOrResultsRow = selectResultsRow(queryId, user)
        if (shouldRespondNow(deadline, afterVersion, troubleOrResultsRow)) {
          //bypass all the concurrent/interrupt business. Just reply.
          completeWithQueryResult(queryId,troubleOrResultsRow)
        }
        else {
          debug(s"Creating promises to respond about $queryId with a version later than $afterVersion by $deadline ")
          // the Promise used to respond
          val okToRespond = Promise[Either[(StatusCode,String),ResultsRow]]()

          //Schedule the timeout
          val okToRespondTimeout = Promise[Unit]()
          okToRespondTimeout.future.transform({unit =>
            okToRespond.tryComplete(Try(selectResultsRow(queryId, user)))
          },{x:Throwable =>  x match {case NonFatal(t) => ExceptionWhilePreparingTimeoutResponse(queryId,t)}
            x
          })
          val timeLeft = (deadline - System.currentTimeMillis()) milliseconds
          case class TriggerRunnable(networkQueryId: NetworkQueryId,promise: Promise[Unit]) extends Runnable {
            val unit:Unit = ()
            override def run(): Unit = promise.trySuccess(unit)
          }
          val timeoutCanceller = system.scheduler.scheduleOnce(timeLeft,TriggerRunnable(queryId,okToRespondTimeout))

          //Set up for an interrupt from new data
          val okToRespondIfNewData = Promise[Unit]()
          okToRespondIfNewData.future.transform({unit =>
            val latestResultsRow = selectResultsRow(queryId, user)
            if(shouldRespondNow(deadline,afterVersion,latestResultsRow)) {
              okToRespond.tryComplete(Try(selectResultsRow(queryId, user)))
            }
          },{x:Throwable =>  x match {case NonFatal(t) => ExceptionWhilePreparingTriggeredResponse(queryId,t)}
            x
          })

          val requestId = UUID.randomUUID()
          //put id -> okToRespondIfNewData in a map so that outside processes can trigger it
          qepQueryDbChangeNotifier.putLongPollRequest(requestId,queryId,okToRespondIfNewData)

          onSuccess(okToRespond.future){ latestResultsRow:Either[(StatusCode,String),ResultsRow] =>
            //clean up concurrent bits before responding
            qepQueryDbChangeNotifier.removeLongPollRequest(requestId)
            timeoutCanceller.cancel()
            completeWithQueryResult(queryId,latestResultsRow)
          }
        }
      }
    }
  }

  /**
    * @param deadline time when a response must go
    * @param afterVersion last timestamp the requester knows about
    * @param resultsRow either the result row or something is not right
    * @return true to respond now, false to dither
    */
  def shouldRespondNow(deadline: Long,
                       afterVersion: Long,
                       resultsRow:Either[(StatusCode,String),ResultsRow]
                      ):Boolean = {
    val currentTime = System.currentTimeMillis()
    if (currentTime >= deadline) true
    else resultsRow.fold(
      {_._1 != StatusCodes.NotFound},
      {_.version > afterVersion}
    )
  }

  def completeWithQueryResult(networkQueryId: NetworkQueryId,troubleOrResultsRow:Either[(StatusCode,String),ResultsRow]): Route = {
    debug(s"Responding to a request for $networkQueryId with $troubleOrResultsRow")
    troubleOrResultsRow.fold({ trouble =>
      //something is wrong. Respond now.
      respondWithStatus(trouble._1) {
        complete(trouble._2)
      }
    }, { queryAndResults =>
      //everything is fine. Respond now.
      val json: Json = Json(queryAndResults)
      val formattedJson: String = Json.format(json)(humanReadable())
      complete(formattedJson)
    })
  }

  def selectResultsRow(queryId:NetworkQueryId,user:User):Either[(StatusCode,String),ResultsRow] = {
    //query once and determine if the latest change > afterVersion

    val queryOption: Option[QepQuery] = QepQueryDb.db.selectQueryById(queryId)
    queryOption.map{query: QepQuery =>
      if (user.sameUserAs(query.userName, query.userDomain)) {
        val mostRecentQueryResults: Seq[Result] = QepQueryDb.db.selectMostRecentFullQueryResultsFor(queryId).map(Result(_))
        val flag = QepQueryDb.db.selectMostRecentQepQueryFlagFor(queryId).map(QueryFlag(_))
        val queryCell = QueryCell(query, flag)
        val queryAndResults = ResultsRow(queryCell, mostRecentQueryResults)

        Right(queryAndResults)
      }
      else Left((StatusCodes.Forbidden,s"Query $queryId belongs to a different user"))
    }.getOrElse(Left[(StatusCode,String),ResultsRow]((StatusCodes.NotFound,s"No query with id $queryId found")))
  }

  def queryResultsTable(user: User): Route = path("queryResultsTable") {

    matchQueryParameters(Some(user.username)){ queryParameters:QueryParameters =>

      val queryRowCount: Int = QepQueryDb.db.countPreviousQueriesByUserAndDomain(
        userName = user.username,
        domain = user.domain
      )

      val queries: Seq[QepQuery] = QepQueryDb.db.selectPreviousQueriesByUserAndDomain(
        userName = user.username,
        domain = user.domain,
        skip = queryParameters.skipOption,
        limit = queryParameters.limitOption
      )
      //todo revisit json structure to remove things the front-end doesn't use
      val adapters: Seq[String] = QepQueryDb.db.selectDistinctAdaptersWithResults

      val flags: Map[NetworkQueryId, QueryFlag] = QepQueryDb.db.selectMostRecentQepQueryFlagsFor(queries.map(q => q.networkId).to[Set])
        .map(q => q._1 -> QueryFlag(q._2))

      val queryResults: Seq[ResultsRow] = queries.map(q => ResultsRow(
        query = QueryCell(q,flags.get(q.networkId)),
        results = QepQueryDb.db.selectMostRecentFullQueryResultsFor(q.networkId).map(Result(_))))

      val table: ResultsTable = ResultsTable(queryRowCount,queryParameters.skipOption.getOrElse(0),adapters,queryResults)

      val jsonTable: Json = Json(table)
      val formattedTable: String = Json.format(jsonTable)(humanReadable())

      complete(formattedTable)
    }
  }

  def matchQueryParameters(userName: Option[UserName])(parameterRoute: QueryParameters => Route): Route = {

    parameters('skip.as[Int].?, 'limit.as[Int].?) { (skipOption, limitOption) =>

      val qp = QueryParameters(
        userName,
        skipOption,
        limitOption
      )
      parameterRoute(qp)
    }
  }
}

//todo maybe move to QepQueryDb class
case class QueryParameters(
                            researcherIdOption:Option[UserName] = None,
                            skipOption:Option[Int] =  None,
                            limitOption:Option[Int] = None
                          )

case class ResultsTable(
  rowCount:Int,
  rowOffset:Int,
  adapters:Seq[String], //todo type for adapter name
  queryResults:Seq[ResultsRow]
)

case class ResultsRow(
                       query:QueryCell,
                       results: Seq[Result],
                       isComplete: Boolean, //a member variable to appear in json
                       version:Long //a time stamp in 1.23, a counting integer in a future release
                      )

object ResultsRow {
  def apply(
             query: QueryCell,
             results: Seq[Result]
           ): ResultsRow = {

    val isComplete = if (results.isEmpty) false
                      else results.forall(_.isComplete)
    val version = (Seq(query.changeDate) ++ results.map(_.changeDate)).max //the latest change date

    ResultsRow(query, results, isComplete, version)
  }
}

case class QueryCell(
                      networkId:String, //easier to support in json, lessens the impact of using a GUID iff we can get there
                      queryName: QueryName,
                      dateCreated: Time,
                      queryXml: String,
                      changeDate: Time,
                      flag:Option[QueryFlag]
                    )

object QueryCell {
  def apply(qepQuery: QepQuery,flag: Option[QueryFlag]): QueryCell = QueryCell(
    networkId = qepQuery.networkId.toString,
    queryName = qepQuery.queryName,
    dateCreated = qepQuery.dateCreated,
    queryXml = qepQuery.queryXml,
    changeDate = qepQuery.changeDate,
    flag
  )
}

case class QueryFlag(
                      flagged:Boolean,
                      flagMessage:String,
                      changeDate:Long
                    )

object QueryFlag{
  def apply(qepQueryFlag: QepQueryFlag): QueryFlag = QueryFlag(qepQueryFlag.flagged, qepQueryFlag.flagMessage, qepQueryFlag.changeDate)
}

case class Result (
  resultId:Long,
  networkQueryId:NetworkQueryId,
  instanceId:Long,
  adapterNode:String,
  resultType:Option[ResultOutputType],
  count:Long,
  status:String, //todo QueryResult.StatusType,
  statusMessage:Option[String],
  changeDate:Long,
  breakdowns: Seq[BreakdownResultsForType],
  problemDigest:Option[ProblemDigestForJson]
) {
  def isComplete = true //todo until I get to SHRINE-2148
}

object Result {
  def apply(fullQueryResult: FullQueryResult): Result = new Result(
    resultId = fullQueryResult.resultId,
    networkQueryId = fullQueryResult.networkQueryId,
    instanceId  = fullQueryResult.instanceId,
    adapterNode = fullQueryResult.adapterNode,
    resultType = fullQueryResult.resultType,
    count = fullQueryResult.count,
    status = fullQueryResult.status.toString,
    statusMessage = fullQueryResult.statusMessage,
    changeDate = fullQueryResult.changeDate,
    breakdowns = fullQueryResult.breakdownTypeToResults.map(tToR => BreakdownResultsForType(fullQueryResult.adapterNode,tToR._1,tToR._2)).to[Seq],
    problemDigest = fullQueryResult.problemDigest.map(ProblemDigestForJson(_))
  )
}

//todo replace when you figure out how to json-ize xml in rapture
case class ProblemDigestForJson(codec: String,
                                stampText: String,
                                summary: String,
                                description: String,
                                detailsString: String,
                                epoch: Long)

object ProblemDigestForJson {
  def apply(problemDigest: ProblemDigest): ProblemDigestForJson = ProblemDigestForJson(
    problemDigest.codec,
    problemDigest.stampText,
    problemDigest.summary,
    problemDigest.description,
    problemDigest.detailsXml.text,
    problemDigest.epoch)
}

case class BreakdownResultsForType(resultType:ResultOutputType,results:Seq[BreakdownResult])

object BreakdownResultsForType {
  def apply(adapterName: String, breakdownType: ResultOutputType, breakdowns: Seq[QepQueryBreakdownResultsRow]): BreakdownResultsForType = {
    val breakdownResults = breakdowns.filter(_.adapterNode == adapterName).map(row => BreakdownResult(row.dataKey,row.value,row.changeDate))

    BreakdownResultsForType(breakdownType,breakdownResults)
  }
}

case class BreakdownResult(dataKey:String,value:Long,changeDate:Long)

case class ExceptionWhilePreparingTriggeredResponse(networkQueryId: NetworkQueryId,x:Throwable) extends AbstractProblem(ProblemSources.Qep) {

  override def throwable = Some(x)

  override def summary: String = "Unable to prepare a triggered response due to an exception."

  override def description: String = s"Unable to prepare a promised response for query $networkQueryId due to a ${x.getClass.getSimpleName}"
}

case class ExceptionWhilePreparingTimeoutResponse(networkQueryId: NetworkQueryId,x:Throwable) extends AbstractProblem(ProblemSources.Qep) {

  override def throwable = Some(x)

  override def summary: String = "Unable to prepare a triggered response due to an exception."

  override def description: String = s"Unable to prepare a promised response for $networkQueryId due to a ${x.getClass.getSimpleName}"
}