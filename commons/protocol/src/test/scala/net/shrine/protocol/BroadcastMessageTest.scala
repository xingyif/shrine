package net.shrine.protocol

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

import net.shrine.util.XmlUtil

/**
 * @author Bill Simons
 * @date 4/5/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class BroadcastMessageTest extends ShouldMatchersForJUnit with XmlSerializableValidator {
  val requestId = 123456
  val masterId = 875875
  val instanceId = 984757
  val resultId1 = 656565
  val resultId2 = 1212121
  val authn = AuthenticationInfo("domain", "username", Credential("cred", false))
  
  import scala.concurrent.duration._
  
  val request = ReadPreviousQueriesRequest("projectId", 10.milliseconds, authn, "username", 20)
  
  val message = XmlUtil.stripWhitespace {
    <broadcastMessage>
      <requestId>{ requestId }</requestId>
      { authn.toXml }
      <request>{ request.toXml }</request>
    </broadcastMessage>
  }

  @Test
  override def testFromXml {
    val actual = BroadcastMessage.fromXml(message).get

    actual.requestId should equal(requestId)
    actual.request should not be (null)
    actual.request.isInstanceOf[ReadPreviousQueriesRequest] should be(true)

    val actualRequest = actual.request.asInstanceOf[ReadPreviousQueriesRequest]

    actualRequest.projectId should equal("projectId")
    actualRequest.waitTime should equal(10.milliseconds)
    actualRequest.authn should equal(authn)
    actualRequest.userId should equal("username")
    actualRequest.fetchSize should equal(20)
  }

  @Test
  override def testToXml {
    BroadcastMessage(requestId, authn, request).toXmlString should equal(message.toString)
  }
}