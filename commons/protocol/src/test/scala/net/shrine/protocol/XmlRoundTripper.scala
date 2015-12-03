package net.shrine.protocol

import org.scalatest.matchers.ShouldMatchers
import net.shrine.serialization.I2b2Marshaller
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.serialization.XmlMarshaller
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Aug 24, 2012
 */
trait XmlRoundTripper[T <: XmlMarshaller with I2b2Marshaller] { self: ShouldMatchersForJUnit =>
  def doShrineXmlRoundTrip(thing: T, unmarshaller: XmlUnmarshaller[T]) {
    val xml = thing.toXmlString
    
    val unmarshalled = unmarshaller.fromXml(xml)
    
    unmarshalled should equal(thing)
  }
  
  def doI2b2XmlRoundTrip(thing: T, unmarshaller: I2b2Unmarshaller[T], equalityChecker: (T, T) => Unit = (_ should equal(_))) {
    val xml = thing.toI2b2String
    
    val unmarshalled = unmarshaller.fromI2b2(xml)
    
    equalityChecker(unmarshalled, thing)
  }
}