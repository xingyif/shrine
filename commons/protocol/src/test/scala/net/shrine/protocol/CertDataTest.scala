package net.shrine.protocol

import net.shrine.util.{Base64, ShouldMatchersForJUnit}
import org.junit.Test
import java.security.cert.X509Certificate
import java.security.cert.CertificateFactory
import java.io.ByteArrayInputStream

/**
 * @author clint
 * @since Dec 4, 2014
 */
final class CertDataTest extends ShouldMatchersForJUnit {
  private val data = CertData("signing cert data".getBytes)
  private val xml = <certData>c2lnbmluZyBjZXJ0IGRhdGE=</certData>

  @Test
  def testToXml: Unit = {
    data.toXmlString should equal(xml.toString)
  }

  @Test
  def testFromXml: Unit = {
    CertData.fromXml(xml).get should equal(data)

    CertData.fromXml(Nil).isFailure should be(true)
    CertData.fromXml(<foo/>).isFailure should be(true)
  }

  @Test
  def testXmlRoundTrip: Unit = {
    CertData.fromXml(data.toXml).get should equal(data)
  }

  @Test
  def testXmlRoundTripRealCert: Unit = {
    val factory = CertificateFactory.getInstance("X.509")
    
    val cert = {
      val encodedCertData = """MIIDjjCCAnagAwIBAgIJAM07lbjzJSv6MA0GCSqGSIb3DQEBBQUAMGcxCzAJBgNV
BAYTAlVTMQswCQYDVQQIDAJNQTEXMBUGA1UECgwOU0hSSU5FIE5ldHdvcmsxGDAW
BgNVBAsMD1NIUklORSBTQU4gVGVzdDEYMBYGA1UEAwwPMTkyLjE2OC4xOTIuMTY3
MB4XDTE1MDExNjIzNTY0OFoXDTE2MDExNjIzNTY0OFoweTELMAkGA1UEBhMCVVMx
CzAJBgNVBAgTAk1BMREwDwYDVQQHEwhBbnl3aGVyZTEXMBUGA1UEChMOU0hSSU5F
IE5ldHdvcmsxGDAWBgNVBAsTD1NIUklORSBTQU4gVGVzdDEXMBUGA1UEAxMOOTIu
MTY4LjE5Mi4xNjgwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCXdXsd
tQVdO7yy6Mxs1N++VCnFsJ+h5RQ82qv65FaPXrjvEyzSx2VgWaCTNKOGSsLQ+MNQ
7OolqlKdkuwkT8NnvumduBlYzEeSistNbiqnfVgUybOz2hLLrsYq2zW2lUxSjp1e
sLVCZ+Re9ug+10IK+2l6ZCDdDnMf4cE1IDwBUmV7RdGZOrXpxKZ4feIYvcplKO02
Vu3BbI9fH3X7AC9N8Ep4FUspMyou87b+oPceLXGPruTy+l1ge0mitoBxB47l3VGn
KNwYLhf29zzDsJOLvb6x1oqkm4yO8i+NKVTt0IqOFh5lC6yDtXzWJ6KB5YCaTfBV
UrgZKgWcg44JXgvNAgMBAAGjKzApMAkGA1UdEwQCMAAwCwYDVR0PBAQDAgXgMA8G
A1UdEQQIMAaHBMCowKgwDQYJKoZIhvcNAQEFBQADggEBAAtVndIIRtH9K67GMzQw
4dzgOcaTy72ZUMeBnt4+gJ0GGfNvaGSzsw3xc2FW9JOcTJ9wzOBsfuAN/d8ArWYk
2Ii1ePjrqgC6DNYViZwVgOCHJ/fM/S3Ie6RL9qpwte1BJUj3BSDvhjAINs2y9Q1c
HHXjFYV/PmMTlz+SoUkUvJKde6hCmdVxYwg0i4sdyD1QxGeYRfd9JYWvHoL0FbTX
6jzhHY9pJgGdSTjO9ENh13693QpXwWMgygNH9bC601YCh5AZi7hUt7k9vNkjy5G2
0FnMchDd6Bb9nxTc0i28yHSdQVvd/X0wqxsVkxMfEAQUNoVNB37CKJq+qWXJ2JuO
BkI="""

      val byteStream = new ByteArrayInputStream(Base64.fromBase64(encodedCertData))

      try { factory.generateCertificate(byteStream).asInstanceOf[X509Certificate] }
      finally { byteStream.close() }
    }
    
    val certData = CertData(cert)
    
    val roundTripped = CertData.fromXml(certData.toXml).get
    
    roundTripped should equal(certData)
    
    val roundTrippedCert = roundTripped.toCertificate
    
    roundTrippedCert should equal(cert)
    roundTrippedCert.getSerialNumber should equal(cert.getSerialNumber)
    roundTrippedCert.getIssuerDN should equal(cert.getIssuerDN)
    roundTrippedCert.getIssuerX500Principal should equal(cert.getIssuerX500Principal)
    roundTrippedCert.getIssuerUniqueID should equal(cert.getIssuerUniqueID)
  }
}