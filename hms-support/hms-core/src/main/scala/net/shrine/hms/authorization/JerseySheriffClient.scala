package net.shrine.hms.authorization

import net.shrine.authorization.AuthorizationResult
import net.shrine.authorization.AuthorizationResult.{NotAuthorized, Authorized}
import net.shrine.log.Loggable
import net.shrine.protocol.ApprovedTopic
import net.shrine.client.JerseyHttpClient
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.client.HttpCredentials
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json.JsonAST.JField
import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.XML
import net.shrine.util.XmlUtil
import scala.xml.Utility
import javax.ws.rs.core.MediaType

/**
 * @author clint
 * @since Apr 3, 2014
 */
final case class JerseySheriffClient(
  sheriffUrl: String,
  sheriffUsername: String,
  sheriffPassword: String) extends SheriffClient {

  import JerseySheriffClient._

  override def getApprovedEntries(ecommonsUsername: String): Seq[ApprovedTopic] = {
    val responseString = resource.path(ecommonsUsername).get(classOf[String])

    parseApprovedTopics(responseString)
  }

  override def isAuthorized(user: String, topicId: String, queryText: String): AuthorizationResult = {
    val responseString = {
      val escapedText = escapeQueryText(queryText)

      //TODO: This was "text/json" in the commons-http-client days, but that doesn't exist for Jersey
      import MediaType.APPLICATION_JSON

      resource.path(s"authorize/$user/$topicId").entity(s"{'queryText': '$escapedText'}", APPLICATION_JSON).post(classOf[String])
    }

    val result = parseAuthorizationResponse(responseString)
    if(result) Authorized(None)
    else NotAuthorized(s"Requested topic $topicId is not approved")
  }

  private[authorization] lazy val resource = {
    import scala.concurrent.duration._

    import JerseyHttpClient._

    //TODO: Don't hardcode timeout here
    //TODO: Take an endpoint, to allow specifying timeouts; 5 minutes is fine for the Sheriff for now.
    val client = createJerseyClient(AcceptAllCerts, 5.minutes)

    createJerseyResource(client, sheriffUrl, Some(HttpCredentials(sheriffUsername, sheriffPassword)))
  }
}

object JerseySheriffClient extends Loggable {
  private implicit val formats = DefaultFormats

  def parseAuthorizationResponse(responseString: String): Boolean = {
    parseJson(responseString).map(json => (json \ "approved").extract[Boolean]).getOrElse(false)
  }

  def parseApprovedTopics(responseString: String): Seq[ApprovedTopic] = {
    parseJson(responseString) match {
      case Some(json) => {
        val transformedJson = json.transform {
          case JField("id", id) => JField("queryTopicId", id)
          case JField("name", name) => JField("queryTopicName", name)
        }

        transformedJson.children.map(_.extract[ApprovedTopic])
      }
      case None => Seq.empty
    }
  }

  def parseJson(jsonString: String): Option[JValue] = {
    import net.liftweb.json.parse

    Try(parse(jsonString)).recover {
      case NonFatal(e) => {
        error(s"Exception parsing JSON '$jsonString': $e", e)

        throw e
      }
    }.toOption
  }

  def escapeQueryText(queryText: String): String = {
    val queryXml = XML.loadString(queryText)

    val trimmedXml = XmlUtil.stripWhitespace(queryXml)

    val escapedText = Utility.escape(trimmedXml.toString())

    escapedText.replace("\\", "\\\\")
  }
}