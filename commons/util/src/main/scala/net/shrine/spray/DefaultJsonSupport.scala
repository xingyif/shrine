package net.shrine.spray

import org.json4s.{DefaultFormats, Formats}
import spray.httpx.Json4sSupport

/**
  * Created by ty on 11/4/16.
  */

trait DefaultJsonSupport extends Json4sSupport {
  override implicit def json4sFormats: Formats = DefaultFormats
}

trait ShaResponse extends DefaultJsonSupport

object ShaResponse extends ShaResponse

case class FoundShaResponse(sha256: String) extends ShaResponse
case class NotFoundShaResponse(sha256: String) extends ShaResponse
case object BadShaResponse extends ShaResponse
