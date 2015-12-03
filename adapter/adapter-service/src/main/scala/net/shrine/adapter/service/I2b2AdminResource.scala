package net.shrine.adapter.service

import net.shrine.log.Loggable

import scala.util.Try
import javax.ws.rs.core.MediaType
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Response
import net.shrine.protocol.HandleableAdminShrineRequest
import net.shrine.protocol.I2b2AdminRequestHandler
import scala.util.Failure
import net.shrine.protocol.ResultOutputType

/**
 * @author clint
 * @date Apr 3, 2013
 */
@Path("/i2b2/admin")
@Produces(Array(MediaType.APPLICATION_XML)) //NB: Is a case class to get an apply method on the companion object, for smoother testing syntax
final case class I2b2AdminResource(i2b2AdminRequestHandler: I2b2AdminRequestHandler, breakdownTypes: Set[ResultOutputType]) extends Loggable {
  //NB: Never broadcast when receiving requests from the legacy i2b2/Shrine webclient, since we can't retrofit it to 
  //Say whether broadcasting is desired for a praticular query/operation
  val shouldBroadcast = false

  @POST
  @Path("request")
  final def doRequest(i2b2Request: String): Response = {
    val builder = HandleableAdminShrineRequest.fromI2b2String(breakdownTypes)(i2b2Request).map {
      shrineRequest =>
        info("Running request from user: %s of type %s".format(shrineRequest.authn.username, shrineRequest.requestType.toString))

        val shrineResponse = shrineRequest.handleAdmin(i2b2AdminRequestHandler, shouldBroadcast)

        val responseString = shrineResponse.toI2b2String

        Response.ok.entity(responseString)
    }.recoverWith {
      case e: Exception => { warn("Error handling request", e); Failure(e) }
    }.getOrElse {
      //TODO: I'm not sure if this is right; need to see what the legacy client expects to be returned in case of an error
      Response.status(400)
    }

    builder.build()
  }
}

//NB: extends I2b2AdminRequestHandler => I2b2AdminResource for smoother testing syntax
object I2b2AdminResource extends ((I2b2AdminRequestHandler, Set[ResultOutputType]) => I2b2AdminResource)