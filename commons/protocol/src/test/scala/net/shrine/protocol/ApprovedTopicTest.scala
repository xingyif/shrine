package net.shrine.protocol

import org.scalatest.junit.AssertionsForJUnit
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import scala.xml.Elem

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
final class ApprovedTopicTest extends ShouldMatchersForJUnit with XmlSerializableValidator {
  @Test
  def testToI2b2 {
    val id = 123L
    val name = "some-name"

    val i2b2Xml = ApprovedTopic(id, name).toI2b2

    i2b2Xml should equal(<sheriffEntry><approval>Approved</approval><queryName>{ name }</queryName><queryTopicID>{ id }</queryTopicID></sheriffEntry>)
  }

  @Test
  override def testToXml {
    val id = 123L
    val name = "some-name"

    val shrineXml = ApprovedTopic(id, name).toXml

    shrineXml should equal(<approvedTopic><queryTopicId>{ id }</queryTopicId><queryTopicName>{ name }</queryTopicName></approvedTopic>)
  }

  @Test
  override def testFromXml {
    val name = "some-name"

    def doTestFromXml(id: Long) {
      val shrineXml = <approvedTopic><queryTopicId>{ id }</queryTopicId><queryTopicName>{ name }</queryTopicName></approvedTopic>

      val topic = ApprovedTopic.fromXml(shrineXml).get

      topic should not be (null)
      topic.queryTopicId should equal(id)
      topic.queryTopicName should equal(name)

      val roundTripped = ApprovedTopic.fromXml(ApprovedTopic(id, name).toXml).get

      roundTripped should not be (null)
      roundTripped should equal(new ApprovedTopic(id, name))
    }

    Seq(-123L, 0L, 123L).foreach(doTestFromXml)
  }
}