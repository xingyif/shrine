package net.shrine.protocol

import scala.util.Random.{ nextLong => randomLong }
import scala.xml.NodeSeq
import org.junit.Test
import net.shrine.util.XmlUtil

/**
 *
 * @author Clint Gilbert
 * @date Sep 19, 2011
 *
 * @link http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 */
final class ReadApprovedQueryTopicsResponseTest extends ShrineResponseValidator {
  private val approvedQueriesResponse = randomResponse(5)

  override val messageBody = XmlUtil.stripWhitespace {
    <message_body>
      <ns7:sheriff_response xmlns="" xsi:type="ns7:sheriffResponseType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        { approvedQueriesResponse.approvedTopics.map(_.toI2b2) }
      </ns7:sheriff_response>
    </message_body>
  }

  import scala.util.Random.{ nextLong => randomLong }

  private def randomApprovedTopic = new ApprovedTopic(randomLong, java.util.UUID.randomUUID.toString)

  private def randomResponse(numTopics: Int) = new ReadApprovedQueryTopicsResponse(for (i <- 1 to numTopics) yield randomApprovedTopic)

  @Test
  def testToI2b2 {
    //we compare the string versions of the xml because Scala's xml equality does not always behave properly
    ReadApprovedQueryTopicsResponse(approvedQueriesResponse.approvedTopics).toI2b2String should equal(response.toString)
  }

  @Test
  def testToI2b2MessageBody {
    Seq(0, 1, 5).foreach { numTopics =>
      val resp = randomResponse(numTopics)

      val i2b2MessageBody = (resp.toI2b2 \\ "message_body" \ "sheriff_response")

      i2b2MessageBody should not be (null)

      //TODO: It would be nice if we could test that needed namespaces end up on the messageBody produced by i2b2MessageBody;
      //this just tests that the approved topics in the i2b2 message body match what's expected.

      //Inlined from older version of ApprovedTopic, because that class doesn't need a fromI2b2 method in its public API
      def approvedTopicFromI2b2(xml: NodeSeq): ApprovedTopic = {
        val topicIdString = (xml \ "queryTopicID").text.trim
        val queryTopicId = topicIdString.toLong
        val queryTopicName = (xml \ "queryName").text

        new ApprovedTopic(queryTopicId, queryTopicName)
      }

      resp.approvedTopics should equal((i2b2MessageBody \ "sheriffEntry").map(approvedTopicFromI2b2))
    }
  }

  private def shrineXml = {
    <readApprovedQueryTopicsResponse><approvedTopics>{ approvedQueriesResponse.approvedTopics.map(_.toXml) }</approvedTopics></readApprovedQueryTopicsResponse>
  }

  @Test
  def testToXml {
    approvedQueriesResponse.toXml should equal(shrineXml)
  }

  @Test
  def testFromXml {
    val resp = ReadApprovedQueryTopicsResponse.fromXml(shrineXml)

    resp should not be (null)

    resp.approvedTopics should equal(approvedQueriesResponse.approvedTopics)
  }
}