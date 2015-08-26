package net.shrine.ont.messaging

import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.shrine.serialization.JsonMarshaller

/**
 * @author Clint Gilbert
 * @date Feb 8, 2012
 */
final case class Blarg(x: Int, y: Double) extends JsonMarshaller {
  override def toJson: JValue = ("x" -> x) ~ ("y" -> y)
}
  
final case class Foo(bar: Int, baz: String, blargs: Seq[Blarg]) extends JsonMarshaller {
  override def toJson: JValue = {
    def blargsArray: JValue = JArray(blargs.map(_.toJson).toList)
    
    ("bar" -> bar) ~ ("baz" -> baz) ~ ("blargs" -> blargsArray)
  }
}