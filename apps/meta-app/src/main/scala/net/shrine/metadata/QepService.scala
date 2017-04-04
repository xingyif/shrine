package net.shrine.metadata

import com.typesafe.config.ConfigRenderOptions
import net.shrine.authorization.steward.{Date, TopicState, UserName}
import net.shrine.i2b2.protocol.pm.User
import net.shrine.log.Loggable
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
      complete(queryParameters.toString)
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