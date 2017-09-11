package net.shrine.messagequeueservice.protocol

import net.shrine.util.Versions
import org.json4s.ShortTypeHints
import org.json4s.native.Serialization

import scala.util.Try
import org.json4s.native.Serialization

/**
  * A json-friendly container for unpacking different messages of known types based on metadata.
  *
  * @author david 
  * @since 9/7/17
  */

//if you need to add fields to this case class they must be Options with default values of None to support serializing to and from JSON
case class Envelope(
                    contentsType:String,
                    contents:String,
                    shrineVersion:String = Versions.version) {

  def decode[T](decoder:String => T):Try[T] = Try{
    decoder(contents)
  }

  def toJson:String = {
    Serialization.write(this)(Envelope.envelopeFormats)
  }

}

object Envelope {
  val envelopeFormats = Serialization.formats(ShortTypeHints(List(classOf[Envelope])))

  def fromJson(jsonString:String):Try[Envelope] = Try{
    implicit val formats = envelopeFormats
    Serialization.read[Envelope](jsonString)
  }
}