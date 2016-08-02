package net.shrine.authorization

import java.net.URL
import javax.net.ssl.{KeyManager, SSLContext, X509TrustManager}
import java.security.cert.X509Certificate

import akka.io.IO
import com.typesafe.config.{Config, ConfigFactory}
import net.shrine.authorization.AuthorizationResult.{Authorized, NotAuthorized}
import net.shrine.authorization.steward.{InboundShrineQuery, ResearchersTopics, TopicIdAndName}
import net.shrine.log.Loggable
import net.shrine.protocol.{ApprovedTopic, AuthenticationInfo, ErrorResponse, ReadApprovedQueryTopicsRequest, ReadApprovedQueryTopicsResponse, RunQueryRequest}
import net.shrine.config.ConfigExtensions
import org.json4s.native.JsonMethods.parse
import org.json4s.{DefaultFormats, Formats}
import akka.actor.ActorSystem
import akka.util.Timeout
import akka.pattern.ask
import net.shrine.problem.{AbstractProblem, ProblemSources}
import spray.can.Http
import spray.can.Http.{HostConnectorInfo, HostConnectorSetup}
import spray.http.{BasicHttpCredentials, HttpRequest, HttpResponse}
import spray.http.StatusCodes.{OK, Unauthorized, UnavailableForLegalReasons}
import spray.httpx.TransformerPipelineSupport.WithTransformation
import spray.httpx.Json4sSupport
import spray.client.pipelining.{Get, Post, addCredentials, sendReceive}
import spray.io.{ClientSSLEngineProvider, PipelineContext, SSLContextProvider}

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

/**
 * A QueryAuthorizationService that talks to the standard data steward application to learn about topics (intents) and check that a
 * shrine query can be run
 *
 * @author david 
 * @since 4/2/15
 */

final case class StewardQueryAuthorizationService(qepUserName:String,
                                                  qepPassword:String,
                                                  stewardBaseUrl:URL,
                                                  defaultTimeout:FiniteDuration = 10 seconds) extends QueryAuthorizationService with Loggable with Json4sSupport {

  import system.dispatcher // execution context for futures
  implicit val system = ActorSystem("AuthorizationServiceActors",ConfigFactory.load("shrine")) //todo use shrine's config

  implicit val timeout:Timeout = Timeout.durationToTimeout(defaultTimeout)//10 seconds

  implicit def json4sFormats: Formats = DefaultFormats

  val qepCredentials = BasicHttpCredentials(qepUserName,qepPassword)

  def sendHttpRequest(httpRequest: HttpRequest):Future[HttpResponse] = {

    // Place a special SSLContext in scope here to be used by HttpClient.
    // It trusts all server certificates.
    // Most important - it will encrypt all of the traffic on the wire.
    implicit def trustfulSslContext: SSLContext = {
      object BlindFaithX509TrustManager extends X509TrustManager {
        def checkClientTrusted(chain: Array[X509Certificate], authType: String) = (info(s"Client asked BlindFaithX509TrustManager to check $chain for $authType"))
        def checkServerTrusted(chain: Array[X509Certificate], authType: String) = (info(s"Server asked BlindFaithX509TrustManager to check $chain for $authType"))
        def getAcceptedIssuers = Array[X509Certificate]()
      }

      val context = SSLContext.getInstance("TLS")
      context.init(Array[KeyManager](), Array(BlindFaithX509TrustManager), null)
      context
    }

    implicit def trustfulSslContextProvider: SSLContextProvider = {
      SSLContextProvider.forContext(trustfulSslContext)
    }

    class CustomClientSSLEngineProvider extends ClientSSLEngineProvider {
      def apply(pc: PipelineContext) = ClientSSLEngineProvider.default(trustfulSslContextProvider).apply(pc)
    }

    implicit def sslEngineProvider: ClientSSLEngineProvider = new CustomClientSSLEngineProvider

    val requestWithCredentials = httpRequest ~> addCredentials(qepCredentials)

    val responseFuture: Future[HttpResponse] = for {
      HostConnectorInfo(hostConnector, _) <- {
        val hostConnectorSetup =  new HostConnectorSetup(httpRequest.uri.authority.host.address,
          httpRequest.uri.authority.port,
          sslEncryption = httpRequest.uri.scheme=="https")(
            sslEngineProvider = sslEngineProvider)

        IO(Http) ask hostConnectorSetup
      }
      response <- sendReceive(hostConnector).apply(requestWithCredentials)
      _ <- hostConnector ask Http.CloseAll
    } yield response

    responseFuture
  }
/*  todo to recycle connections with http://spray.io/documentation/1.2.3/spray-client/ if needed
  def sendHttpRequest(httpRequest: HttpRequest):Future[HttpResponse] = {
  import akka.io.IO
import akka.pattern.ask
import spray.can.Http

    val requestWithCredentials = httpRequest ~> addCredentials(qepCredentials)
    //todo failures via onFailure callbacks
    for{
      sendR:SendReceive <- connectorSource
      response:HttpResponse <- sendR(requestWithCredentials)
    } yield response
  }
  val connectorSource: Future[SendReceive] = //Future[HttpRequest => Future[HttpResponse]]
    for (
      //keep asking for a connector until you get one
      //todo correct URL
//        Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup("www.spray.io", port = 8080)
      Http.HostConnectorInfo(connector, _) <- IO(Http) ? Http.HostConnectorSetup("localhost", port = 6060)
    ) yield sendReceive(connector)
*/

  def sendAndReceive(httpRequest: HttpRequest,timeout:Duration = defaultTimeout):HttpResponse = {
    info("StewardQueryAuthorizationService will request "+httpRequest.uri) //todo someday log request and response

    val responseFuture = sendHttpRequest(httpRequest)

    val response:HttpResponse = Await.result(responseFuture,timeout)

    info("StewardQueryAuthorizationService received response with status "+response.status)

    response
  }

  //Contact a data steward and either return an Authorized or a NotAuthorized or throw an exception
  override def authorizeRunQueryRequest(runQueryRequest: RunQueryRequest): AuthorizationResult = {
    debug(s"authorizeRunQueryRequest started for ${runQueryRequest.queryDefinition.name}")

    val interpreted = runQueryRequest.topicId.fold(
      authorizeRunQueryRequestNoTopic(runQueryRequest)
    )(
      authorizeRunQueryRequestForTopic(runQueryRequest,_)
    )

    debug(s"authorizeRunQueryRequest completed with $interpreted) for ${runQueryRequest.queryDefinition.name}")

    interpreted
  }

  def authorizeRunQueryRequestNoTopic(runQueryRequest: RunQueryRequest): AuthorizationResult = {
    val userName = runQueryRequest.authn.username
    val queryId = runQueryRequest.queryDefinition.name

    //xml's .text returns something that looks like xquery with backwards slashes. toString() returns xml.
    val queryForJson = InboundShrineQuery(runQueryRequest.networkQueryId,queryId,runQueryRequest.queryDefinition.toXml.toString())

    val request = Post(s"$stewardBaseUrl/steward/qep/requestQueryAccess/user/$userName", queryForJson)
    val response:HttpResponse = sendAndReceive(request,runQueryRequest.waitTime)

    interpretAuthorizeRunQueryResponse(response)
  }

  def authorizeRunQueryRequestForTopic(runQueryRequest: RunQueryRequest,topicIdString:String): AuthorizationResult = {
    val userName = runQueryRequest.authn.username
    val queryId = runQueryRequest.queryDefinition.name

    //xml's .text returns something that looks like xquery with backwards slashes. toString() returns xml.
    val queryForJson = InboundShrineQuery(runQueryRequest.networkQueryId,queryId,runQueryRequest.queryDefinition.toXml.toString())

    val request = Post(s"$stewardBaseUrl/steward/qep/requestQueryAccess/user/$userName/topic/$topicIdString", queryForJson)
    val response:HttpResponse = sendAndReceive(request,runQueryRequest.waitTime)

    debug(s"authorizeRunQueryRequestForTopic response is $response")

    interpretAuthorizeRunQueryResponse(response)
  }

/** Interpret the response from the steward app. Primarily here for testing. */
  def interpretAuthorizeRunQueryResponse(response:HttpResponse):AuthorizationResult = {
    response.status match {
      case OK => {
        val topicJson = new String(response.entity.data.toByteArray)
        debug(s"topicJson is $topicJson")

        val topic:Option[TopicIdAndName] = parse(topicJson).extractOpt[TopicIdAndName]
        debug(s"topic is $topic")

        Authorized(topic.map(x => (x.id,x.name)))
      }
      case UnavailableForLegalReasons => NotAuthorized(response.entity.asString)
      case Unauthorized => throw new AuthorizationException(s"steward rejected qep's login credentials. $response")
      case _ => throw new AuthorizationException(s"QueryAuthorizationService detected a problem: $response")
    }
  }

  //Either read the approved topics from a data steward or have an error response.
  override def readApprovedEntries(readTopicsRequest: ReadApprovedQueryTopicsRequest): Either[ErrorResponse, ReadApprovedQueryTopicsResponse] = {
    val userName = readTopicsRequest.authn.username

    val request = Get(s"$stewardBaseUrl/steward/qep/approvedTopics/user/$userName")

    val response:HttpResponse = sendAndReceive(request,readTopicsRequest.waitTime)

    if(response.status == OK) {

      val topicsJson = new String(response.entity.data.toByteArray)
      val topicsFromSteward: ResearchersTopics = parse(topicsJson).extract[ResearchersTopics]

      val topics: Seq[ApprovedTopic] = topicsFromSteward.topics.map(topic => ApprovedTopic(topic.id, topic.name))

      Right(ReadApprovedQueryTopicsResponse(topics))
    }
    else Left(ErrorResponse(ErrorStatusFromDataStewardApp(response,stewardBaseUrl)))
  }

  override def toString() = {
    super.toString().replaceAll(qepPassword,"REDACTED")
  }
}

object StewardQueryAuthorizationService {

  def apply(config:Config):StewardQueryAuthorizationService = StewardQueryAuthorizationService (
    qepUserName = config.getString("qepUserName"),
    qepPassword = config.getString("qepPassword"),
    stewardBaseUrl = config.get("stewardBaseUrl", new URL(_))
  )
}

case class ErrorStatusFromDataStewardApp(response:HttpResponse,stewardBaseUrl:URL) extends AbstractProblem(ProblemSources.Qep) {
  override lazy val summary: String = s"Data Steward App responded with status ${response.status}"
  override lazy val description:String = s"The Data Steward App at ${stewardBaseUrl} responded with status ${response.status}, not OK."
  override lazy val detailsXml = <details>
    Response is {response}
    {throwableDetail.getOrElse("")}
  </details>

}