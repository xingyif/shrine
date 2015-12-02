package net.shrine.status

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization

import net.shrine.log.Loggable

/**
  * A subservice that shares internal state of the shrine servlet.
  *
  * @author david 
  * @since 12/2/15
  */
@Path("/status")
@Produces(Array(MediaType.APPLICATION_JSON))
case class StatusJaxrs() extends Loggable {

  implicit def json4sFormats: Formats = DefaultFormats

  @GET
  @Path("version")
  def version: String = {
    val version = Version("changeMe")
    val versionString = Serialization.write(version)
    debug(s"Reported version $versionString")
    versionString
  }


}

case class Version(version:String)