package net.shrine.protocol

import xml.{ NodeSeq, XML }
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlUtil
import net.shrine.serialization.{ I2b2Unmarshaller, XmlUnmarshaller }
import net.shrine.util.XmlDateHelper

/**
 * @author Bill Simons
 * @date 4/11/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final case class ReadQueryDefinitionResponse(
    val masterId: Long,
    val name: String,
    val userId: String,
    val createDate: XMLGregorianCalendar,
    val queryDefinition: String) extends ShrineResponse {

  override protected def i2b2MessageBody = XmlUtil.stripWhitespace {
    <ns6:response xsi:type="ns6:master_responseType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <status>
        <condition type="DONE">DONE</condition>
      </status>
      <query_master>
        <query_master_id>{ masterId }</query_master_id>
        <name>{ name }</name>
        <user_id>{ userId }</user_id>
        <create_date>{ createDate }</create_date>
        <request_xml>{ XML.loadString(queryDefinition) }</request_xml>
      </query_master>
    </ns6:response>
  }

  override def toXml = XmlUtil.stripWhitespace {
    <readQueryDefinitionResponse>
      <masterId>{ masterId }</masterId>
      <name>{ name }</name>
      <userId>{ userId }</userId>
      <createDate>{ createDate }</createDate>
      <queryDefinition>{ queryDefinition }</queryDefinition>
    </readQueryDefinitionResponse>
  }

  override def canEqual(other: Any): Boolean = other.isInstanceOf[ReadQueryDefinitionResponse]

  //NB: Does not include create date in equality
  override def equals(other: Any): Boolean = {
    other match {
      case that: ReadQueryDefinitionResponse => (that canEqual this) &&
        masterId == that.masterId &&
        name == that.name &&
        userId == that.userId &&
        queryDefinition == that.queryDefinition
      case _ => false
    }
  }

  //NB: Does not include create date in hashCode
  override def hashCode: Int = 41 * (41 * (41 * (41 + masterId.hashCode) + name.hashCode) + userId.hashCode) + queryDefinition.hashCode
}

object ReadQueryDefinitionResponse extends I2b2Unmarshaller[ReadQueryDefinitionResponse] with XmlUnmarshaller[ReadQueryDefinitionResponse] {
  override def fromI2b2(nodeSeq: NodeSeq) = {
    val queryMasterXml = nodeSeq \ "message_body" \ "response" \ "query_master"

    ReadQueryDefinitionResponse(
      (queryMasterXml \ "query_master_id").text.toLong,
      (queryMasterXml \ "name").text,
      (queryMasterXml \ "user_id").text,
      XmlDateHelper.parseXmlTime((queryMasterXml \ "create_date").text).get, //NB: Preserve old exception-throwing behavior for now
      (queryMasterXml \ "request_xml" \ "query_definition").toString)
  }

  override def fromXml(nodeSeq: NodeSeq) = {
    ReadQueryDefinitionResponse(
      (nodeSeq \ "masterId").text.toLong,
      (nodeSeq \ "name").text,
      (nodeSeq \ "userId").text,
      XmlDateHelper.parseXmlTime((nodeSeq \ "createDate").text).get, //NB: Preserve old exception-throwing behavior for now
      (nodeSeq \ "queryDefinition").text)
  }
}