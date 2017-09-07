package net.shrine.messagequeueservice.protocol

import net.shrine.util.Versions

import scala.util.Try

/**
  * A json-friendly container for unpacking different messages of known types based on metadata.
  *
  * @author david 
  * @since 9/7/17
  */

//if you need to add fields to this case class they must be Options with default values of None to support serializing to JSON
case class Envelope(contentsType:String,contents:String,shrineVersion:String = Versions.version) {

  def decode[T](decoder:String => T):Try[T] = Try{
    decoder(contents)
  }

}
