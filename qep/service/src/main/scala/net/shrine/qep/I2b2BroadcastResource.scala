package net.shrine.qep

import java.sql.SQLException
import javax.ws.rs.{POST, Path, Produces}
import javax.ws.rs.core.{MediaType, Response}
import javax.ws.rs.core.Response.ResponseBuilder

import net.shrine.authentication.NotAuthenticatedException
import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, ProblemNotYetEncoded, ProblemSources}
import net.shrine.protocol.{ErrorResponse, HandleableI2b2Request, I2B2MessageFormatException, I2b2RequestHandler, ResultOutputType, ShrineRequest}
import net.shrine.qep.querydb.QepDatabaseProblem
import net.shrine.serialization.I2b2Marshaller
import net.shrine.slick.{CouldNotRunDbIoActionException, DbIoActionException}
import net.shrine.util.XmlUtil
import org.xml.sax.SAXParseException

import scala.util.Try
import scala.util.control.NonFatal
import scala.xml.NodeSeq

/**
 * @author Bill Simons
 * @since 3/10/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
@Path("/i2b2")
@Produces(Array(MediaType.APPLICATION_XML))
final case class I2b2BroadcastResource(i2b2RequestHandler: I2b2RequestHandler, breakdownTypes: Set[ResultOutputType]) extends Loggable {

  //NB: Always broadcast when receiving requests from the legacy i2b2/Shrine webclient, since we can't retrofit it to 
  //Say whether broadcasting is desired for a praticular query/operation
  val shouldBroadcast = true

  @POST
  @Path("request")
  def doRequest(i2b2Request: String): Response = processI2b2Message(i2b2Request)

  @POST
  @Path("pdorequest")
  def doPDORequest(i2b2Request: String): Response = processI2b2Message(i2b2Request)

  def processI2b2Message(i2b2Request: String): Response = {
    // todo would be good to log $i2b2Request)")

    def errorResponse(e: Throwable): ErrorResponse = e match {
      case nax:NotAuthenticatedException => ErrorResponse(nax.problem)
      case dbioax:DbIoActionException => ErrorResponse(QepDatabaseProblem(dbioax))
      case sqlx:SQLException => ErrorResponse(QepDatabaseProblem(sqlx))
      case imfx:I2B2MessageFormatException => ErrorResponse(QepCouldNotInterpretRequest(i2b2Request,imfx))
      case saxx:SAXParseException => ErrorResponse(QepCouldNotInterpretRequest(i2b2Request,saxx))
      case _ => ErrorResponse(ProblemNotYetEncoded("The QEP encountered an unforeseen problem while processing an i2b2 request",e))
    }

    def prettyPrint(xml: NodeSeq): String = XmlUtil.prettyPrint(xml.head).trim
    
    //NB: The legacy webclient can't deal with non-200 status codes.  
    //It also can't deal with ErrorResponses in several cases, but we have nothing better to return for now.
    //TODO: Return a 500 status here, once we're using the new web client
    def i2b2HttpErrorResponse(e: Throwable): ResponseBuilder = Response.ok.entity(prettyPrint(errorResponse(e).toI2b2))

    def handleRequest(shrineRequest: ShrineRequest with HandleableI2b2Request): Try[ResponseBuilder] = Try {
      info(s"Running request from user: ${shrineRequest.authn.username} of type ${shrineRequest.requestType.toString}")

      val shrineResponse = shrineRequest.handleI2b2(i2b2RequestHandler, shouldBroadcast)

      //TODO: Revisit this.  For now, we bail if we get something that isn't i2b2able
      val responseString: String = shrineResponse match {
        case i2b2able: I2b2Marshaller => prettyPrint(i2b2able.toI2b2)
        case _ => throw new Exception(s"Shrine response $shrineResponse has no i2b2 representation")
      }

      Response.ok.entity(responseString)
    }.recover {
      case NonFatal(e) =>
        error("Error processing request: ", e)
        i2b2HttpErrorResponse(e)
    }

    def handleParseError(e: Throwable): Try[ResponseBuilder] = Try {
      debug(s"Failed to unmarshal i2b2 request $i2b2Request")

      error("Couldn't understand request: ", e)

      //NB: The legacy webclient can't deal with non-200 status codes.  
      //It also can't deal with ErrorResponses in several cases, but we have nothing better to return for now.
      //TODO: Return a 400 status here, once we're using the new web client
      i2b2HttpErrorResponse(e)
    }

    val builder = HandleableI2b2Request.fromI2b2String(breakdownTypes)(i2b2Request).transform(handleRequest, handleParseError).get

    builder.build()
  }
}

case class QepCouldNotInterpretRequest(i2b2Request:String,x:Exception) extends AbstractProblem(ProblemSources.Qep){
  override val summary = "The QEP could not interpret a request."

  override val throwable = Some(x)

  override val description = x.getMessage

  override val detailsXml: NodeSeq = NodeSeq.fromSeq(
    <details>
      Request contents:'{i2b2Request}'

      {throwableDetail.getOrElse("")}
    </details>
  )
}