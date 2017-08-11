package net.shrine.metadata

import akka.actor.{ActorSystem, Cancellable}
import net.shrine.audit.{NetworkQueryId, QueryName, Time}
import net.shrine.authorization.steward.UserName
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.problem.ProblemDigest
import net.shrine.protocol.ResultOutputType
import net.shrine.qep.querydb.{FullQueryResult, QepQuery, QepQueryBreakdownResultsRow, QepQueryDb, QepQueryFlag}
import spray.routing._
import rapture.json._
import rapture.json.jsonBackends.jawn._
import rapture.json.formatters.humanReadable
import spray.http.{StatusCode, StatusCodes}

import scala.concurrent.duration._
import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}
import scala.concurrent.Promise
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

/**
  * An API to support the web client's work with queries.
  *
  * The current API supplies information about previous running queries. Eventually this will support accessing
  * information about queries running now and the ability to submit queries.
  */

//todo move this to the qep/service module
trait QepService extends HttpService with Loggable {

  def system: ActorSystem

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

//todo key should also have something to do with the request. Maybe everything. Triggerer will need to scan the whole set of keys to decide what to trigger, or maybe try triggering everything

  //todo can this promise be Promise[Unit] ?
  val longPollRequestsToComplete:ConcurrentMap[NetworkQueryId,(Promise[NetworkQueryId],Cancellable)] = TrieMap.empty

  case class TriggerRunnable(networkQueryId: NetworkQueryId,promise: Promise[NetworkQueryId]) extends Runnable {
    override def run(): Unit = promise.trySuccess(networkQueryId)
  }

  def respondWithQueryResult(troubleOrResultsRow:Either[(StatusCode,String),ResultsRow]): Route = {
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

//todo update long poll description
  /*
When a request comes in

if the request can be fulfilled immediately then do that
if not
create a promise who's future's success has an on-success to call respondOrWait
schedule a task to fulfil that promise in Akka's scheduler (and produce a cancellable)
put queryId -> (promise, cancelable) into the concurrent map

when new information comes in
grab the right promise from the map
promise.trySuccess (triggering onSuccess)
maybe cancel the cancellable (may not need to)

if the scheduler runs the runnable
promise.trySuccess (triggering onSuccess)
OK to cancel the cancellable (because it is already running)
*/
  def queryResult(user:User):Route = path("queryResult" / LongNumber) { queryId: NetworkQueryId =>

    //take optional parameters for version and an awaitTime
    //todo use a Duration ? or timeoutSeconds ?
    //If the timeout parameter isn't supplied then the deadline is now so it replies immediately
    parameters('afterVersion.as[Long] ? 0L, 'timeout.as[Long] ? 0L) { (afterVersion: Long, timeout: Long) =>

      val requestStartTime = System.currentTimeMillis()
      val deadline = requestStartTime + timeout

      detach(){
        val troubleOrResultsRow = selectResultsRow(queryId, user)
        if (shouldRespondNow(deadline, afterVersion, troubleOrResultsRow)) {
          //bypass all the concurrent/interrupt business. Just reply.
          respondWithQueryResult(troubleOrResultsRow)
        }
        else {

          val triggerAndCancel = longPollRequestsToComplete.getOrElseUpdate(queryId,{
            val trigger = Promise[NetworkQueryId]()
            val timeLeft = (deadline - System.currentTimeMillis()) milliseconds
            val cancellable:Cancellable = system.scheduler.scheduleOnce(timeLeft,TriggerRunnable(queryId,trigger))
            (trigger,cancellable)
          })

          //todo be sure it is to respond - Add another future to see if it will be ok to respond.
          onSuccess(triggerAndCancel._1.future){ betterMatchQueryId =>

            //todo check if shouldRespondNow. Otherwise keep waiting
            val latestResultsRow = selectResultsRow(queryId, user)
            triggerAndCancel._2.cancel()
            respondWithQueryResult(latestResultsRow)
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
  //todo use Deadline instead of Long?
  def shouldRespondNow(deadline: Long,
                       afterVersion: Long,
                       resultsRow:Either[(StatusCode,String),ResultsRow]
                      ):Boolean = {
    val currentTime = System.currentTimeMillis()
    if (currentTime >= deadline) true
    else resultsRow.fold(
      {_._1 != StatusCodes.NotFound},
      {_.latestChange > afterVersion}
    )
  }

  def selectResultsRow(queryId:NetworkQueryId,user:User):Either[(StatusCode,String),ResultsRow] = {
    //query once and determine if the latest change > afterVersion

    val queryOption: Option[QepQuery] = QepQueryDb.db.selectQueryById(queryId)
    queryOption.fold {
      //todo only complete if deadline is past. Otherwise reschedule
      val left:Either[(StatusCode,String),ResultsRow] = Left[(StatusCode,String),ResultsRow]((StatusCodes.NotFound,s"No query with id $queryId found"))
      left
    }
    { query: QepQuery =>
      if (user.sameUserAs(query.userName, query.userDomain)) {
        val mostRecentQueryResults: Seq[Result] = QepQueryDb.db.selectMostRecentFullQueryResultsFor(queryId).map(Result(_))
        val flag = QepQueryDb.db.selectMostRecentQepQueryFlagFor(queryId).map(QueryFlag(_))
        val queryCell = QueryCell(query, flag)
        val queryAndResults = ResultsRow(queryCell, mostRecentQueryResults)

        Right(queryAndResults)
      }
      else Left((StatusCodes.Forbidden,s"Query $queryId belongs to a different user"))
    }
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
                            limitOption:Option[Int] = None //todo deadline, maybe version, someday
                          )

case class ResultsTable(
  rowCount:Int,
  rowOffset:Int,
  adapters:Seq[String], //todo type for adapter name
  queryResults:Seq[ResultsRow]
)

case class ResultsRow(
                       query:QueryCell,
                       results: Seq[Result]
                      ) {
  def latestChange:Long = (Seq(query.changeDate) ++ results.map(_.changeDate)).max
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
)

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
