package net.shrine.client

import net.shrine.log.Loggable
import net.shrine.protocol.{HiveCredentials, ShrineRequest, AuthenticationInfo, Credential, RequestType}
import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import scala.concurrent.duration.Duration
import net.shrine.crypto.TrustParam
import scala.xml.XML
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import net.shrine.util.NodeSeqEnrichments

/**
 * @author clint
 * @date Jan 24, 2014
 */
final class PosterOntClient(hiveCredentials: HiveCredentials, waitTime: Duration, poster: Poster) extends OntClient {

  override def childrenOf(parent: String): Set[String] = {
    import scala.concurrent.duration._
    import PosterOntClient._

    val urlSuffixForThisOp = "getChildren"

    val posterForThisOp = {
      def addTrailingSlash(s: String) = if (s.endsWith("/")) s else s + "/"

      if (poster.url.endsWith(urlSuffixForThisOp)) poster
      else poster.copy(url = addTrailingSlash(poster.url) + urlSuffixForThisOp)
    }

    val authn = hiveCredentials.toAuthenticationInfo

    val message = ReadOntChildNodesRequest(hiveCredentials.projectId, waitTime, authn, parent)

    val response = posterForThisOp.post(message.toI2b2String)

    //extractChildTermsFromI2b2Response(XML.loadString(response.body))
    extractChildTermsFromI2b2Response(XmlUtil.loadStringIgnoringRemoteResources(response.body))
  }
}

object PosterOntClient extends Loggable {

  def extractChildTermsFromI2b2Response(xmlOption: Option[NodeSeq]): Set[String] = {
    import NodeSeqEnrichments.Strictness._

    xmlOption match {
      case None => Set.empty
      case Some(xml) => {
        val childrenAttempt = for {
          childNodes <- xml withChild "message_body" withChild "concepts" withChild "concept" withChild "key"
          childStrings = childNodes.map(_.text.trim).toSet
        } yield childStrings

        childrenAttempt match {
          case Success(children) => children
          case Failure(e) => {
            error(s"Error couldn't extract ontology terms from '$xml'", e)

            Set.empty
          }
        }
      }
    }
  }

  final case class ReadOntChildNodesRequest(
    override val projectId: String,
    override val waitTime: Duration,
    override val authn: AuthenticationInfo,
    parent: String) extends ShrineRequest(projectId, waitTime, authn) {

    override val requestType = RequestType.ReadOntChildTerms

    override def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
      <message_body>
        <ns100:get_children blob="true" type="default" max='100' synonyms="false" hiddens="true">
          <parent>{ parent }</parent>
        </ns100:get_children>
      </message_body>
    }

    //NB: Will never be serialized in Shrine format, as these messages are only destined for the I2b2 Ont cell. 
    override def toXml: NodeSeq = ???
  }
}