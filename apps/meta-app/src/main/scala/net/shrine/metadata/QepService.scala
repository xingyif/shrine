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


/**
  * An API to support the web client's work with queries.
  *
  * The current API supplies information about previous running queries. Eventually this will support accessing
  * information about queries running now and the ability to submit queries.
  */

//todo move this to the qep/service module
trait QepService extends HttpService with Loggable {

//  def system: ActorSystem

  val qepInfo =
    """
      |The SHRINE query entry point service.
      |
      |This API gives a researcher access to queries, and (eventually) the ability to run queries.
      |
    """.stripMargin

  def qepRoute(user: User): Route = pathPrefix("qep") {
    get {
      queryResult(user) ~ queryResultsTable(user)
    } ~
      pathEndOrSingleSlash{complete(qepInfo)} ~
      respondWithStatus(StatusCodes.NotFound){complete(qepInfo)}
  }


  val longPollRequestsToComplete:ConcurrentMap[NetworkQueryId,(Runnable,Cancellable)] = TrieMap.empty

  case class SelectsResults(queryId:NetworkQueryId,afterVersion:Long,deadline:Long) extends Runnable {
    override def run(): Unit = {
      //cancel this runnable



    }
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

  def queryResult(user:User):Route = path("queryResult" / LongNumber) { queryId: NetworkQueryId =>

    //take optional parameters for version and an awaitTime
    //todo use a Duration
    parameters('afterVersion.as[Long] ? 0L, 'timeout.as[Long] ? 0L) { (afterVersion: Long, timeout: Long) =>

      val requestStartTime = System.currentTimeMillis()
      val deadline = requestStartTime + timeout

      //query once and determine if the latest change > afterVersion

      val troubleOrResultsRow = selectResultsRow(queryId,user)

      troubleOrResultsRow match {
        case Right(queryAndResults) =>
          //only complete if deadline is past or latest change > afterVersion
          val currentTime = System.currentTimeMillis()
          if((queryAndResults.latestChange > afterVersion) || (currentTime > deadline)) {
            val json: Json = Json(queryAndResults)
            val formattedJson: String = Json.format(json)(humanReadable())
            complete(formattedJson)
          }
          else {
            //todo reschedule at deadline
            ??? //             complete("Delay doing this for a bit")
          }

        case Left((statusCode,message)) =>
          if(statusCode == StatusCodes.NotFound) {
            //todo only complete if deadline is past. Otherwise reschedule
            respondWithStatus(statusCode){complete(message)}
          } else {
            //Any other problems, go ahead and complete
            respondWithStatus(statusCode){complete(message)}
          }
      }
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
