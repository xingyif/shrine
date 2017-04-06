package net.shrine.metadata

import net.shrine.audit.NetworkQueryId
import net.shrine.authorization.steward.{Date, TopicState, UserName}
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.protocol.ResultOutputType
import net.shrine.qep.queries.{FullQueryResult, QepQuery, QepQueryDb, QepQueryFlag}
import spray.routing.{HttpService, _}

/**
  * An API to support the web client's work with queries.
  *
  * The current API supplies information about previous running queries. Eventually this will support accessing
  * information about queries running now and the ability to submit queries.
  */

//todo  maybe move this to the qep/service module
trait QepService extends HttpService with Loggable {
  val qepInfo =
    """
      |The SHRINE query entry point service.
      |
      |This API gives a researcher access to queries, and (eventually) the ability to run queries.
      |
      |
      |
      |This is a simple API that gives you
      |read access to the metaData section within SHRINE's configuration.
      |You can access this data by key, or by accessing the entire metaData
      |config section at once. To access everything at once, make a GET
      |to shrine-metadata/data (if on a browser, just add /data to the
      |end of the current url). To access values by key, make a GET to
      |shrine-metadata/data?key={{your key here without braces}} (again,
      |if on a browser just add /data?key={{your key}} to the end of the url).
    """.stripMargin


  def qepRoute(user: User): Route = pathPrefix("qep") {
    get {
      queryResults(user)
    }
  } ~ complete(qepInfo)

  def queryResults(user: User): Route = pathPrefix("queryResults") {
    matchQueryParameters(Some(user.username)){ queryParameters:QueryParameters =>

      val queries: Seq[QepQuery] = QepQueryDb.db.selectPreviousQueriesByUserAndDomain(
        userName = user.username,
        domain = user.domain,
        skip = queryParameters.skipOption,
        limit = queryParameters.limitOption
      )

      //todo revisit json structure to remove things the front-end doesn't use
      val adapters: Seq[String] = QepQueryDb.db.selectDistinctAdaptersWithResults

      val queryResults: Seq[ResultsRow] = queries.map(q => ResultsRow(q,QepQueryDb.db.selectMostRecentFullQueryResultsFor(q.networkId).map(Result(_))))

      val flags: Map[NetworkQueryId, QepQueryFlag] = QepQueryDb.db.selectMostRecentQepQueryFlagsFor(queries.map(q => q.networkId).to[Set])

      val table: ResultsTable = ResultsTable(adapters,queryResults,flags)
      import rapture.json._
      import rapture.json.formatters.humanReadable._
      import rapture.json.jsonBackends.jawn._
      import rapture._

      val jsonTable: Json = Json(table)

      complete(jsonTable.toString())
//      complete(Json.format(jsonTable).toString)
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

//todo move to QepQueryDb class
case class QueryParameters(
                            researcherIdOption:Option[UserName] = None,
                            skipOption:Option[Int] =  None,
                            limitOption:Option[Int] = None //todo deadline, maybe version, someday
                          )

case class ResultsTable(
  adapters:Seq[String], //todo type for adapter name
  rows:Seq[ResultsRow],
  flags:Map[NetworkQueryId,QepQueryFlag]
)

case class ResultsRow(
  query:QepQuery,
  adaptersToResults: Seq[Result]
)

case class Result (
  resultId:Long,
  networkQueryId:NetworkQueryId,
  instanceId:Long,
  adapterNode:String,
  resultType:Option[ResultOutputType],
  count:Long,
  startDate:Option[Long],
  endDate:Option[Long],
  status:String, //todo QueryResult.StatusType,
  statusMessage:Option[String],
  changeDate:Long
// todo   breakdowns:Option[Map[ResultOutputType,I2b2ResultEnvelope]]
// todo  problemDigest:Option[ProblemDigest]
)

object Result {
  def apply(fullQueryResult: FullQueryResult): Result = Result(
    resultId = fullQueryResult.resultId,
    networkQueryId = fullQueryResult.networkQueryId,
    instanceId  = fullQueryResult.instanceId,
    adapterNode = fullQueryResult.adapterNode,
    resultType = fullQueryResult.resultType,
    count = fullQueryResult.count,
    startDate = fullQueryResult.startDate,
    endDate = fullQueryResult.endDate,
    status = fullQueryResult.status.toString,
    statusMessage = fullQueryResult.statusMessage,
    changeDate = fullQueryResult.changeDate
//    breakdowns = fullQueryResult.breakdowns
//    problemDigest = fullQueryResult.problemDigest
  )
}
