package net.shrine.adapter

import net.shrine.problem.{ProblemSources, AbstractProblem}
import org.xml.sax.SAXParseException

import scala.xml.NodeSeq
import scala.xml.XML
import net.shrine.protocol.{HiveCredentials, AuthenticationInfo, BroadcastMessage, Credential, ShrineRequest, ShrineResponse, TranslatableRequest, BaseShrineRequest, ErrorResponse, BaseShrineResponse}
import net.shrine.util.XmlDateHelper
import net.shrine.client.Poster
import scala.util.Try
import scala.util.control.NonFatal

/**
 * @author Bill Simons
 * @since 4/11/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
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
      shrineResponse <- Try(parseShrineResponse(crcXml)).recover { case NonFatal(e) =>
        info(s"Exception while parsing $crcXml",e)
        ErrorResponse.fromI2b2(crcXml)
      } //todo pass the exception to build a proper error response, and log the exception
    } yield shrineResponse

    shrineResponseAttempt.recover {
      case saxx:SAXParseException => ErrorResponse(CannotParseXmlFromCrc(saxx,xmlResponseFromCrc))
      case NonFatal(e) =>
        error(s"Error parsing response from CRC: ", e)

        ErrorResponse(ExceptionWhileLoadingCrcResponse(e,xmlResponseFromCrc))
    }.get
  }

  //NB: default is a noop; only RunQueryAdapter needs this for now
  protected[adapter] def translateNetworkToLocal(request: T): T = request

  protected[adapter] override def processRequest(message: BroadcastMessage): BaseShrineResponse = {
    val i2b2Response = callCrc(translateRequest(message.request))

    parseShrineErrorResponseWithFallback(i2b2Response)
  }

  protected def callCrc(request: ShrineRequest): String = {
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
    case transReq: TranslatableRequest[T] => //noinspection RedundantBlock
    {
      val HiveCredentials(domain, username, password, project) = hiveCredentials

      val authInfo = AuthenticationInfo(domain, username, Credential(password, isToken = false))

      translateNetworkToLocal(transReq.withAuthn(authInfo).withProject(project).asRequest)
    }
    case req: ShrineRequest => req
    case _ => throw new IllegalArgumentException(s"Unexpected request: $request")
  }
}

case class CannotParseXmlFromCrc(saxx:SAXParseException,xmlResponseFromCrc: String) extends AbstractProblem(ProblemSources.Adapter) {
  override val throwable = Some(saxx)
  override val summary: String = "Could not parse response from CRC."
  override val description:String = s"${saxx.getMessage} while parsing the response from the CRC."
  override val detailsXml = <details>
    {throwableDetail.getOrElse("")}
    Response is {xmlResponseFromCrc}
  </details>
}

case class ExceptionWhileLoadingCrcResponse(t:Throwable,xmlResponseFromCrc: String) extends AbstractProblem(ProblemSources.Adapter) {
  override val throwable = Some(t)
  override val summary: String = "Unanticipated exception with response from CRC."
  override val description:String = s"${t.getMessage} while parsing the response from the CRC."
  override val detailsXml = <details>
    {throwableDetail.getOrElse("")}
    Response is {xmlResponseFromCrc}
  </details>
}