package net.shrine.metadata

import com.typesafe.config.ConfigRenderOptions
import net.shrine.audit.NetworkQueryId
import net.shrine.authorization.steward.{Date, TopicState, UserName}
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
import net.shrine.qep.queries.{FullQueryResult, QepQuery, QepQueryDb, QepQueryFlag}
import net.shrine.source.ConfigSource
import spray.http.{StatusCode, StatusCodes}
import spray.routing.{HttpService, _}

import scala.util.{Failure, Success, Try}

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

      //todo revisit json structure to remove junk
      val flags: Map[NetworkQueryId, QepQueryFlag] = QepQueryDb.db.selectMostRecentQepQueryFlagsFor(queries.map(q => q.networkId).to[Set])
      val queryResults: Map[NetworkQueryId, Map[String, FullQueryResult]] = queries.flatMap(q => QepQueryDb.db.selectMostRecentFullQueryResultsFor(q.networkId)).groupBy(_.networkQueryId).map(q => q._1 -> q._2.groupBy(_.adapterNode).map(r => r._1 -> r._2.head))
      val adapters: Seq[String] = QepQueryDb.db.selectDistinctAdaptersWithResults

      val table = ResultsTable(adapters,queries,flags,queryResults)

      complete(table.toString)
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
  queries:Seq[QepQuery],
  flags:Map[NetworkQueryId,QepQueryFlag],
  queryResults:Map[NetworkQueryId,Map[String,FullQueryResult]]
)