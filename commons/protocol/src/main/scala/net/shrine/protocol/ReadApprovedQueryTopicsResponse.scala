package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.util.{Tries, XmlUtil}
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try

/**
 * @author Bill Simons
 * @since 8/29/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 * 
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class ReadApprovedQueryTopicsResponse(val approvedTopics: Seq[ApprovedTopic]) extends ShrineResponse {
  override protected def i2b2MessageBody = XmlUtil.stripWhitespace {
    <ns7:sheriff_response xsi:type="ns7:sheriffResponseType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      { approvedTopics.map(_.toI2b2) }
    </ns7:sheriff_response>
  }

  override def toXml = XmlUtil.stripWhitespace {
    <readApprovedQueryTopicsResponse>
      <approvedTopics>
        { approvedTopics.map(_.toXml) }
      </approvedTopics>
    </readApprovedQueryTopicsResponse>
  }
}

object ReadApprovedQueryTopicsResponse extends XmlUnmarshaller[ReadApprovedQueryTopicsResponse] with HasRootTagName {
  override val rootTagName = "readApprovedQueryTopicsResponse"
  
  override def fromXml(xml: NodeSeq): ReadApprovedQueryTopicsResponse = {
    val approvedTopicNodes = for {
      approvedTopicsXml <- xml \\ "approvedTopic"
      approvedTopicNode <- approvedTopicsXml
    } yield approvedTopicNode
    
    val resultAttempt = for {
      approvedTopics <- Tries.sequence(approvedTopicNodes.map(ApprovedTopic.fromXml))
    } yield {
      ReadApprovedQueryTopicsResponse(approvedTopics)
    }
    
    //NB: Preserve old exception-throwing behavior for now
    resultAttempt.get
  }
}