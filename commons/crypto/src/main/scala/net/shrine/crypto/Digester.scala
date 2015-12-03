package net.shrine.crypto

import net.shrine.protocol.ShrineResponse
import net.shrine.serialization.XmlMarshaller

/**
 * @author clint
 * @date Nov 25, 2013
 * 
 * A type class describing how we turn any needed types into byte arrays for marshalling.
 * The advantage of the type class is that the digesting code can be external to the 
 * classes/types being digested; ie, we don't need to add a toDigest method to 
 * BroadcastMessage.   
 * 
 * NB: Contravariant type param allows using XmlMarshallersAreDigestable to digest any 
 * subclass of XmlMarshaller without explicit casting or type ascription.
 */
trait Digester[-T] {
  def digest(t: T): Array[Byte]
}

object Digester {
  implicit val XmlMarshallersAreDigestable: Digester[XmlMarshaller] = new Digester[XmlMarshaller] {
    override def digest(thing: XmlMarshaller): Array[Byte] = thing.toXmlString.getBytes("UTF-8")
  }
}