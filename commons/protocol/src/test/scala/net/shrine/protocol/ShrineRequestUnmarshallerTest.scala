package net.shrine.protocol

import junit.framework.TestCase
import org.junit.Test
import net.shrine.util.XmlUtil
import scala.xml.NodeSeq
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Aug 16, 2012
 */
final class ShrineRequestUnmarshallerTest extends TestCase with ShouldMatchersForJUnit {
  private val projectId = "jksahjksafhkafkla"
    
  import scala.concurrent.duration._
    
  private val waitTime = 12345.milliseconds
  
  private val authn = AuthenticationInfo("some-domain", "some-username", Credential("ksaljfksadlfjklsd", false))
  
  private val xml = XmlUtil.stripWhitespace(
    <foo>
      <projectId>{ projectId }</projectId>
      <waitTimeMs>{ waitTime.toMillis}</waitTimeMs>
      { authn.toXml }
    </foo>)
  
  final class Foo
    
  private object MockUnmarshaller extends ShrineRequestUnmarshaller
    
  @Test
  def testShrineHeader {
    val RequestHeader(actualProjectId, actualWaitTimeMs, actualAuthn) = MockUnmarshaller.shrineHeader(xml).get
    
    actualProjectId should equal(projectId)
    actualWaitTimeMs should equal(waitTime)
    actualAuthn should equal(authn)
  }
  
  @Test
  def testShrineProjectId {
    MockUnmarshaller.shrineProjectId(xml).get should equal(projectId)
  }
  
  @Test
  def testShrineWaitTimeMs {
    MockUnmarshaller.shrineWaitTime(xml).get should equal(waitTime)
  }
  
  @Test
  def testShrineAuthenticationInfo {
    MockUnmarshaller.shrineAuthenticationInfo(xml).get should equal(authn)
  }
}