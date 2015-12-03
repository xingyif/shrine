package net.shrine.broadcaster.service

import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.POST
import javax.ws.rs.Consumes
import net.shrine.protocol.{SingleNodeResult, BroadcastMessage, MultiplexedResults, ResultOutputType}
import javax.ws.rs.core.Response
import scala.util.control.NonFatal
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.XML
import net.shrine.util.StringEnrichments

/**
 * @author clint
 * @date Feb 28, 2014
 */
@Path("broadcaster")
@Produces(Array(MediaType.APPLICATION_XML))
final case class BroadcasterMultiplexerResource(handler: BroadcasterMultiplexerRequestHandler) {
  @POST
  @Path("/broadcast")
  def broadcastAndMultiplex(broadcastMessageXml: String): Response = {
    import StringEnrichments._

    val messageAttempt = broadcastMessageXml.tryToXml.flatMap(BroadcastMessage.fromXml)

    val responseBuilder = {
      if (messageAttempt.isFailure) { Response.status(400) }
      else {
        messageAttempt.map { message =>
          val results: Iterable[SingleNodeResult] = handler.broadcastAndMultiplex(message)

          val resultsXml = MultiplexedResults(results.toSeq).toXmlString

          Response.ok.entity(resultsXml)
        }.getOrElse {
          Response.status(500)
        }
      }
    }

    responseBuilder.build
  }
}