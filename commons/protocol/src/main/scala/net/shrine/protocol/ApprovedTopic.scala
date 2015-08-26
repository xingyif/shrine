package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.{I2b2Marshaller, XmlMarshaller}
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.XmlUnmarshaller


/**
 * @author Bill Simons
 * @date 8/29/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 * 
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class ApprovedTopic(queryTopicId: Long, queryTopicName: String) extends XmlMarshaller with I2b2Marshaller {
  override def toI2b2: NodeSeq = XmlUtil.stripWhitespace {
    <sheriffEntry>
      <approval>Approved</approval>
      <queryName>{queryTopicName}</queryName>
      <queryTopicID>{queryTopicId}</queryTopicID>
    </sheriffEntry>
  }

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <approvedTopic>
      <queryTopicId>{queryTopicId}</queryTopicId>
      <queryTopicName>{queryTopicName}</queryTopicName>
    </approvedTopic>
  }
}

object ApprovedTopic extends XmlUnmarshaller[Try[ApprovedTopic]] {
  override def fromXml(xml: NodeSeq): Try[ApprovedTopic] = {
    import NodeSeqEnrichments.Strictness._
    
    for {
      queryTopicId <- (xml withChild "queryTopicId").map(_.text.trim.toLong)
      queryTopicName <- (xml withChild "queryTopicName").map(_.text.trim)
    } yield {
      ApprovedTopic(queryTopicId, queryTopicName)
    }
  }
}