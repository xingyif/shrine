package net.shrine.metadata

import net.shrine.audit.{NetworkQueryId, QueryName, Time}
import net.shrine.authorization.steward.UserName
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.problem.ProblemDigest
import net.shrine.protocol.ResultOutputType
import net.shrine.qep.querydb.{FullQueryResult, QepQuery, QepQueryDb, QepQueryFlag}
import spray.routing._
import rapture.json._
import rapture.json.jsonBackends.jawn._
import rapture.json.formatters.humanReadable
import spray.http.StatusCodes

/**
  * An API to support the web client's work with queries.
  *
  * The current API supplies information about previous running queries. Eventually this will support accessing
  * information about queries running now and the ability to submit queries.
  */

//todo move this to the qep/service module
trait QepService extends HttpService with Loggable {
  val qepInfo =
    """
      |The SHRINE query entry point service.
      |
      |This API gives a researcher access to queries, and (eventually) the ability to run queries.
      |
    """.stripMargin


  def qepRoute(user: User): Route = pathPrefix("qep") {
    get {
      queryResultsTable(user)
    } ~
      pathEndOrSingleSlash{complete(qepInfo)} ~
      respondWithStatus(StatusCodes.NotFound){complete(qepInfo)}
  }

  def queryResultsTable(user: User): Route = pathPrefix("queryResultsTable") {

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
        adaptersToResults = QepQueryDb.db.selectMostRecentFullQueryResultsFor(q.networkId).map(Result(_))))

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
  adaptersToResults: Seq[Result]
)

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
// todo   breakdowns:Option[Map[ResultOutputType,I2b2ResultEnvelope]]
  problemDigest:Option[ProblemDigestForJson]
)

object Result {
  def apply(fullQueryResult: FullQueryResult): Result = Result(
    resultId = fullQueryResult.resultId,
    networkQueryId = fullQueryResult.networkQueryId,
    instanceId  = fullQueryResult.instanceId,
    adapterNode = fullQueryResult.adapterNode,
    resultType = fullQueryResult.resultType,
    count = fullQueryResult.count,
    status = fullQueryResult.status.toString,
    statusMessage = fullQueryResult.statusMessage,
    changeDate = fullQueryResult.changeDate,
//    breakdowns = fullQueryResult.breakdowns
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