package net.shrine.spray

import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport

/**
  * Created by ty on 11/4/16.
  */

trait DefaultJsonSupport extends Json4sSupport {
  override implicit def json4sFormats: Formats = DefaultFormats
}

case class ShaResponse(sha256: String, found:Boolean) extends DefaultJsonSupport
object ShaResponse extends DefaultJsonSupport {
  val badFormat = "Bad format"
}