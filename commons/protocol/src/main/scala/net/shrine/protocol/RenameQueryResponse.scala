package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.{I2b2Unmarshaller, XmlUnmarshaller}

/**
 * @author Bill Simons
 * @date 4/11/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class RenameQueryResponse(val queryId: Long, val queryName: String) extends ShrineResponse {
  override protected def i2b2MessageBody = XmlUtil.stripWhitespace(
    <ns6:response xsi:type="ns6:master_responseType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <status>
        <condition type="DONE">DONE</condition>
      </status>
      <query_master>
        <query_master_id>{queryId}</query_master_id>
        <name>{queryName}</name>
      </query_master>
    </ns6:response>)

  def withId(id: Long): RenameQueryResponse = this.copy(queryId = id)

  override def toXml = XmlUtil.stripWhitespace(
    <renameQueryResponse>
      <queryId>{queryId}</queryId>
      <queryName>{queryName}</queryName>
    </renameQueryResponse>)
}

object RenameQueryResponse extends I2b2Unmarshaller[RenameQueryResponse] with XmlUnmarshaller[RenameQueryResponse] {
  override def fromI2b2(nodeSeq: NodeSeq) = new RenameQueryResponse(
    (nodeSeq \ "message_body" \ "response" \ "query_master" \ "query_master_id").text.toLong,
    (nodeSeq \ "message_body" \ "response" \ "query_master" \ "name").text)

  override def fromXml(nodeSeq: NodeSeq) = new RenameQueryResponse(
    (nodeSeq \ "queryId").text.toLong,
    (nodeSeq \ "queryName").text)
}