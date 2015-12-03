package net.shrine.status

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

import com.typesafe.config.{Config => TsConfig, ConfigValue}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.native.Serialization

import net.shrine.log.Loggable

import scala.collection.JavaConverters._
import scala.collection.immutable.{Map,Set}

/**
  * A subservice that shares internal state of the shrine servlet.
  *
  * @author david 
  * @since 12/2/15
  */
@Path("/status")
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
