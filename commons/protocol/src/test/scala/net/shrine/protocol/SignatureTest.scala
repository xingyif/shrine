package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.XmlDateHelper
import java.math.BigInteger
import net.shrine.util.XmlGcEnrichments

/**
 * @author clint
 * @date Dec 3, 2013
 */
final class SignatureTest extends ShouldMatchersForJUnit {
  private val certId = CertId(new BigInteger("12345"))
  
  private val now = XmlDateHelper.now
  
  private val sig = Signature(now, certId, None, "aslkjfhalskfhlkasf".getBytes)
  
  private val caSig = Signature(now, certId, Some(CertData("signing cert data".getBytes)), "aslkjfhalskfhlkasf".getBytes)
  
  @Test
  def testToXml {
    sig.toXmlString should equal(s"<signature><timestamp>${now.toString}</timestamp><signedBy><serial>12345</serial></signedBy><value>YXNsa2pmaGFsc2tmaGxrYXNm</value></signature>")
    
    caSig.toXmlString should equal(s"<signature><timestamp>${now.toString}</timestamp><signedBy><serial>12345</serial></signedBy><signingCert>c2lnbmluZyBjZXJ0IGRhdGE=</signingCert><value>YXNsa2pmaGFsc2tmaGxrYXNm</value></signature>")
  }

  @Test
  def testFromXml {
    import Signature.fromXml

    fromXml(Nil).isFailure should be(true)

    fromXml(<foo/>).isFailure should be(true)
    
    fromXml(s"<signature><timestamp>${now.toString}</timestamp><signedBy><serial>12345</serial></signedBy><value>YXNsa2pmaGFsc2tmaGxrYXNm</value></signature>").get should equal(sig)
    
    fromXml(s"<signature><timestamp>${now.toString}</timestamp><signedBy><serial>12345</serial></signedBy><signingCert>c2lnbmluZyBjZXJ0IGRhdGE=</signingCert><value>YXNsa2pmaGFsc2tmaGxrYXNm</value></signature>").get should equal(caSig)
  }

  @Test
  def testXmlRoundTrip {
    Signature.fromXml(sig.toXml).get should equal(sig)
    
    Signature.fromXml(caSig.toXml).get should equal(caSig)
  }
  
  @Test
  def testEquals {
    sig should equal(sig)
    caSig should equal(caSig)
    sig should not equal(caSig)
    caSig should not equal(sig)
    
    sig should equal(Signature(now, CertId(new BigInteger("12345")), None, "aslkjfhalskfhlkasf".getBytes))
    Signature(now, CertId(new BigInteger("12345")), None, "aslkjfhalskfhlkasf".getBytes) should equal(sig)
    
    caSig should equal(Signature(now, certId, Some(CertData("signing cert data".getBytes)), "aslkjfhalskfhlkasf".getBytes))
    Signature(now, certId, Some(CertData("signing cert data".getBytes)), "aslkjfhalskfhlkasf".getBytes) should equal(caSig)
    
    import scala.concurrent.duration._
    import XmlGcEnrichments._
    
    sig.copy(timestamp = now + 1.second) should not equal(sig)
    sig should not equal(sig.copy(timestamp = now + 1.second))
    
    caSig.copy(timestamp = now + 1.second) should not equal(caSig)
    caSig should not equal(caSig.copy(timestamp = now + 1.second))
    
    sig.copy(signedBy = CertId(new BigInteger("938794857"))) should not equal(sig)
    sig should not equal(sig.copy(signedBy = CertId(new BigInteger("938794857"))))
    
    caSig.copy(signedBy = CertId(new BigInteger("938794857"))) should not equal(caSig)
    caSig should not equal(caSig.copy(signedBy = CertId(new BigInteger("938794857"))))
    
    sig.copy(value = Array[Byte](0x00, 0x01, 0x02)) should equal(sig.copy(value = Array[Byte](0x00, 0x01, 0x02)))
    caSig.copy(value = Array[Byte](0x00, 0x01, 0x02)) should equal(caSig.copy(value = Array[Byte](0x00, 0x01, 0x02)))
    
    sig should not equal(sig.copy(value = Array[Byte](0x00, 0x01, 0x02)))
    sig.copy(value = Array[Byte](0x00, 0x01, 0x02)) should not equal(sig)
    
    caSig should not equal(caSig.copy(value = Array[Byte](0x00, 0x01, 0x02)))
    caSig.copy(value = Array[Byte](0x00, 0x01, 0x02)) should not equal(caSig)
    
    caSig should not equal(caSig.copy(signingCert = None))
    caSig.copy(signingCert = None) should not equal(caSig)
  }
  
  @Test
  def testHashCode {
    sig.hashCode should equal(sig.hashCode)
    caSig.hashCode should equal(caSig.hashCode)
    sig.hashCode should not equal(caSig.hashCode)
    caSig.hashCode should not equal(sig.hashCode)
    
    sig.hashCode should equal(Signature(now, CertId(new BigInteger("12345")), None, "aslkjfhalskfhlkasf".getBytes).hashCode)
    Signature(now, CertId(new BigInteger("12345")), None, "aslkjfhalskfhlkasf".getBytes).hashCode should equal(sig.hashCode)
    
    caSig.hashCode should equal(Signature(now, certId, Some(CertData("signing cert data".getBytes)), "aslkjfhalskfhlkasf".getBytes).hashCode)
    Signature(now, certId, Some(CertData("signing cert data".getBytes)), "aslkjfhalskfhlkasf".getBytes).hashCode should equal(caSig.hashCode)
    
    import scala.concurrent.duration._
    import XmlGcEnrichments._
    
    sig.copy(timestamp = now + 1.second).hashCode should not equal(sig.hashCode)
    sig.hashCode should not equal(sig.copy(timestamp = now + 1.second).hashCode)
    
    caSig.copy(timestamp = now + 1.second).hashCode should not equal(caSig.hashCode)
    caSig.hashCode should not equal(caSig.copy(timestamp = now + 1.second).hashCode)
    
    sig.copy(signedBy = CertId(new BigInteger("938794857"))).hashCode should not equal(sig.hashCode)
    sig.hashCode should not equal(sig.copy(signedBy = CertId(new BigInteger("938794857"))).hashCode)
    
    caSig.copy(signedBy = CertId(new BigInteger("938794857"))).hashCode should not equal(caSig.hashCode)
    caSig.hashCode should not equal(caSig.copy(signedBy = CertId(new BigInteger("938794857"))).hashCode)
    
    sig.copy(value = Array[Byte](0x00, 0x01, 0x02)).hashCode should equal(sig.copy(value = Array[Byte](0x00, 0x01, 0x02)).hashCode)
    caSig.copy(value = Array[Byte](0x00, 0x01, 0x02)).hashCode should equal(caSig.copy(value = Array[Byte](0x00, 0x01, 0x02)).hashCode)
    
    sig.hashCode should not equal(sig.copy(value = Array[Byte](0x00, 0x01, 0x02)).hashCode)
    sig.copy(value = Array[Byte](0x00, 0x01, 0x02)).hashCode should not equal(sig.hashCode)
    
    caSig.hashCode should not equal(caSig.copy(value = Array[Byte](0x00, 0x01, 0x02)).hashCode)
    caSig.copy(value = Array[Byte](0x00, 0x01, 0x02)).hashCode should not equal(caSig.hashCode)
    
    caSig.hashCode should not equal(caSig.copy(signingCert = None).hashCode)
    caSig.copy(signingCert = None).hashCode should not equal(caSig.hashCode)
  }
}