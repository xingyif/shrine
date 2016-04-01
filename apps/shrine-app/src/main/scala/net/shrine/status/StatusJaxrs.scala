package net.shrine.status

import javax.ws.rs.{WebApplicationException, GET, Path, Produces}
import javax.ws.rs.core.{Response, MediaType}

import com.sun.jersey.spi.container.{ContainerRequest, ContainerRequestFilter}
import com.typesafe.config.{Config => TsConfig}
import net.shrine.wiring.ShrineOrchestrator
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization

import net.shrine.log.Loggable

import scala.collection.JavaConverters._
import scala.collection.immutable.{Map,Set}

import net.shrine.config.ConfigExtensions

/**
  * A subservice that shares internal state of the shrine servlet.
  *
  * @author david 
  * @since 12/2/15
  */
@Path("/internalstatus")
@Produces(Array(MediaType.APPLICATION_JSON))
case class StatusJaxrs(shrineConfig:TsConfig) extends Loggable {

  implicit def json4sFormats: Formats = DefaultFormats

  @GET
  @Path("version")
  def version: String = {
    val version = Version("changeMe")
    val versionString = Serialization.write(version)
    versionString
  }

  @GET
  @Path("config")
  def config: String = {
    //todo probably better to reach out and grab the config from ManuallyWiredShrineJaxrsResources once it is a singleton
    Serialization.write(Json4sConfig(shrineConfig))
  }
}

case class Version(version:String)

//todo SortedMap
case class Json4sConfig(keyValues:Map[String,String]){

}

object Json4sConfig{
  def isPassword(key:String):Boolean = {
    if(key.toLowerCase.contains("password")) true
    else false
  }

  def apply(config:TsConfig):Json4sConfig = {

    val entries: Set[(String, String)] = config.entrySet.asScala.to[Set].map(x => (x.getKey,x.getValue.render())).filterNot(x => isPassword(x._1))
    val sortedMap: Map[String, String] = entries.toMap
    Json4sConfig(sortedMap)
  }
}

class PermittedHostOnly extends ContainerRequestFilter {

  //todo generalize for happy, too
  //todo for tomcat 8 see https://jersey.java.net/documentation/latest/filters-and-interceptors.html for a cleaner version
  //shell code from http://stackoverflow.com/questions/17143514/how-to-add-custom-response-and-abort-request-in-jersey-1-11-filters

  //how to apply in http://stackoverflow.com/questions/4358213/how-does-one-intercept-a-request-during-the-jersey-lifecycle
  override def filter(requestContext: ContainerRequest): ContainerRequest = {
    val hostOfOrigin = requestContext.getBaseUri.getHost
    val shrineConfig:TsConfig = ShrineOrchestrator.config
    val permittedHostOfOrigin:String = shrineConfig.getOption("shrine.status.permittedHostOfOrigin",_.getString).getOrElse("localhost")

    val path = requestContext.getPath

    //happy and internalstatus API calls must come from the same host as tomcat is running on (hopefully the dashboard servlet).
    // todo access to the happy service permitted for SHRINE 1.21 per SHRINE-1366
    // restrict access to happy service when database work resumes as part of SHRINE-
    //       if ((path.contains("happy") || path.contains("internalstatus")) && (hostOfOrigin != permittedHostOfOrigin)) {
    if (( path.contains("internalstatus")) && (hostOfOrigin != permittedHostOfOrigin)) {
      val response = Response.status(Response.Status.UNAUTHORIZED).entity(s"Only available from $permittedHostOfOrigin, not $hostOfOrigin, controlled by shrine.status.permittedHostOfOrigin in shrine.conf").build()
      throw new WebApplicationException(response)
    }
    else requestContext
  }

}