package net.shrine.messagequeueservice.protocol

import net.shrine.util.Versions
import org.json4s.ShortTypeHints
import org.json4s.native.Serialization

import scala.util.{Failure, Success, Try}

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

  def checkVersionExactMatch: Try[Envelope] = {
    if(shrineVersion == Versions.version) Success(this)
    else Failure(VersionMismatchException(shrineVersion))
  }
}

case class VersionMismatchException(badVersion:String) extends Exception(s"Cannot use an Envelope with version $badVersion in ${Versions.version}")

object Envelope {
  val envelopeFormats = Serialization.formats(ShortTypeHints(List(classOf[Envelope])))

  def fromJson(jsonString:String):Try[Envelope] = Try{
    implicit val formats = envelopeFormats
    Serialization.read[Envelope](jsonString)
  }
}