package net.shrine.ont.messaging

import net.liftweb.json._
import net.shrine.serialization.JsonUnmarshaller

/**
 * @author Clint Gilbert
 * @date Feb 8, 2012
 */
abstract class LiftJsonUnmarshaller[T : Manifest] extends JsonUnmarshaller[T] {
  final override def fromJson(json: JValue): T = json.extract[T](DefaultFormats, manifest[T])
}