package net.shrine.protocol

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import java.math.BigInteger

/**
 * @author clint
 * @date Dec 2, 2013
 */
final class CertIdTest extends ShouldMatchersForJUnit {
  private def bigInt(i: Int): BigInteger = BigInteger.valueOf(i)
  
  private val serialInt = 123
  
  private lazy val serial = bigInt(serialInt)
  
  private lazy val certId = CertId(serial)
  
  private val name = "foo"
  
  private lazy val certIdWithName = CertId(serial, Option(name))
  
  @Test
  def testToXml {
    certId.toXmlString should equal(s"<certId><serial>$serialInt</serial></certId>")
    
    certIdWithName.toXmlString should equal(s"<certId><serial>$serialInt</serial><name>$name</name></certId>")
  }
  
  @Test
  def testXmlRoundTrip {
    import CertId.fromXml
    
    {
      val unmarshalled = fromXml(certId.toXml).get
      
      unmarshalled.serial should equal(serial)
      unmarshalled.name should be(None)
    }
    
    {
      val unmarshalled = fromXml(certIdWithName.toXml).get
      
      unmarshalled.serial should equal(serial)
      unmarshalled.name should be(Some("foo"))
    }
  }
  
  @Test
  def testFromBadXml {
    import CertId.fromXml
    
    fromXml(null).isFailure should be(true)
    
    //TODO: It would be nice if fromXml(String) caught errors in XML parsing and wrapped them in a Try
    intercept[Exception] {
      fromXml("").isFailure should be(true)
    }
    
    //TODO: It would be nice if fromXml(String) caught errors in XML parsing and wrapped them in a Try
    intercept[Exception] {
      fromXml("askhdfalsf").isFailure should be(true)
    }
    
    fromXml(<certId><bar>{serialInt}</bar><baz>{name}</baz></certId>).isFailure should be(true)
  }
}