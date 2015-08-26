package net.shrine.adapter

import scala.xml.NodeSeq
import scala.xml.XML
import net.shrine.protocol.{HiveCredentials, AuthenticationInfo, BroadcastMessage, Credential, ShrineRequest, ShrineResponse, TranslatableRequest, BaseShrineRequest, ErrorResponse, BaseShrineResponse}
import net.shrine.serialization.XmlMarshaller
import net.shrine.client.HttpClient
import net.shrine.util.XmlDateHelper
import net.shrine.client.Poster
import net.shrine.util.XmlUtil
import net.shrine.client.HttpResponse
import scala.util.Try
import scala.util.control.NonFatal

/**
 * @author Bill Simons
 * @date 4/11/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
abstract class CrcAdapter[T <: ShrineRequest, V <: ShrineResponse](
  poster: Poster,
  override protected val hiveCredentials: HiveCredentials) extends WithHiveCredentialsAdapter(hiveCredentials) {

  protected def parseShrineResponse(nodeSeq: NodeSeq): ShrineResponse

  private[adapter] def parseShrineErrorResponseWithFallback(xmlResponseFromCrc: String): ShrineResponse = {
    //NB: See https://open.med.harvard.edu/jira/browse/SHRINE-534
    //NB: https://open.med.harvard.edu/jira/browse/SHRINE-745
    val shrineResponseAttempt = for {
      crcXml <- Try(XML.loadString(xmlResponseFromCrc))
      shrineResponse <- Try(parseShrineResponse(crcXml)).recover { case NonFatal(e) => ErrorResponse.fromI2b2(crcXml) }
    } yield shrineResponse

    shrineResponseAttempt.recover {
      case NonFatal(e) =>
        error(s"Error parsing response from CRC: ", e)

        ErrorResponse(s"Error parsing response from CRC; $e")
    }.get
  }

  //NB: default is a noop; only RunQueryAdapter needs this for now
  protected[adapter] def translateNetworkToLocal(request: T): T = request

  protected[adapter] override def processRequest(message: BroadcastMessage): BaseShrineResponse = {
    val i2b2Response = callCrc(translateRequest(message.request))

    parseShrineErrorResponseWithFallback(i2b2Response)
  }

  protected def callCrc(request: ShrineRequest): String = {
    def prettyPrintXmlString(reqXml: String): Try[String] = Try {
      XmlUtil.prettyPrint(XML.loadString(reqXml))
    }

    def prettyPrintResponse(resp: HttpResponse): Try[HttpResponse] = {
      prettyPrintXmlString(resp.body).map(prettyPrintedXml => resp.copy(body = s"\r\n$prettyPrintedXml"))
    }

    debug(s"Sending Shrine-formatted request to the CRC at '${poster.url}': $request")

    val crcRequest = request.toI2b2String

    val crcResponse = XmlDateHelper.time(s"Calling the CRC at '${poster.url}'")(debug(_)) {
      //Wrap exceptions in a more descriptive form, to enable sending better error messages back to the legacy web client
      try { poster.post(crcRequest) }
      catch {
        case NonFatal(e) => throw CrcInvocationException(poster.url, request, e)
      }
    }

    crcResponse.body
  }

  private[adapter] def translateRequest(request: BaseShrineRequest): ShrineRequest = request match {
    case transReq: TranslatableRequest[T] => {
      val HiveCredentials(domain, username, password, project) = hiveCredentials

      val authInfo = AuthenticationInfo(domain, username, Credential(password, false))

      translateNetworkToLocal(transReq.withAuthn(authInfo).withProject(project).asRequest)
    }
    case req: ShrineRequest => req
    case _ => throw new IllegalArgumentException(s"Unexpected request: $request")
  }
}