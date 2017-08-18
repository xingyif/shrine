package net.shrine.broadcaster.service

import javax.ws.rs.{POST, Path, Produces}
import javax.ws.rs.core.{MediaType, Response}

import net.shrine.protocol.{BroadcastMessage, MultiplexedResults, SingleNodeResult}
import net.shrine.util.StringEnrichments

/**
 * @author clint
 * @since Feb 28, 2014
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