package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.{Tries, XmlUtil, XmlDateHelper, NodeSeqEnrichments}
import net.shrine.serialization.{ I2b2Unmarshaller, XmlUnmarshaller }
import scala.util.Try

/**
 * @author Bill Simons
 * @since 4/11/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class ReadPreviousQueriesResponse(queryMasters: Seq[QueryMaster]) extends ShrineResponse {

  override def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    <ns5:response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:master_responseType">
      <status>
        <condition type="DONE">DONE</condition>
      </status>
      {
        for {
          master <- queryMasters
        } yield {
          <query_master>
            <query_master_id>{ master.queryMasterId }</query_master_id>
            <network_query_id>{ master.networkQueryId }</network_query_id>
            <name>{ master.name }</name>
            <user_id>{ master.userId }</user_id>
            <group_id>{ master.groupId }</group_id>
            <create_date>{ master.createDate }</create_date>
            { master.flagged.map(f => <flagged>{ f }</flagged>).orNull }
            { master.flagMessage.map(f => <flagMessage>{ f }</flagMessage>).orNull }
          </query_master>
        }
      }
    </ns5:response>
  }

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <readPreviousQueriesResponse>
      {
        queryMasters.map { master =>
          <queryMaster>
            <masterId>{ master.queryMasterId }</masterId>
            <networkId>{ master.networkQueryId }</networkId>
            <name>{ master.name }</name>
            <createDate>{ master.createDate }</createDate>
            <userId>{ master.userId }</userId>
            <groupId>{ master.groupId }</groupId>
            { master.flagged.map(f => <flagged>{ f }</flagged>).orNull }
            { master.flagMessage.map(f => <flagMessage>{ f }</flagMessage>).orNull }
          </queryMaster>
        }
      }
    </readPreviousQueriesResponse>
  }
}

object ReadPreviousQueriesResponse extends I2b2Unmarshaller[ReadPreviousQueriesResponse] with XmlUnmarshaller[ReadPreviousQueriesResponse] {
  override def fromI2b2(xml: NodeSeq): ReadPreviousQueriesResponse = {
    //NB: Fail fast
    require((xml \ "response_header" \ "result_status" \ "status" \ "@type").text == "DONE")

    doFromXml("query_master_id", "network_query_id", "name", "user_id", "group_id", "create_date", "held", "flagged", "flagMessage")(xml \ "message_body" \ "response" \ "query_master")
  }

  override def fromXml(xml: NodeSeq): ReadPreviousQueriesResponse = doFromXml("masterId", "networkId", "name", "userId", "groupId", "createDate", "held", "flagged", "flagMessage")(xml \ "queryMaster")
  
  private[this] def doFromXml(queryMasterIdTagName: String, networkQueryIdTagName: String, nameTagName: String, userIdTagName: String, groupIdTagName: String, createDateTagName: String, heldTagName: String, flaggedTagName: String, flagMessageTagName: String)(xml: NodeSeq): ReadPreviousQueriesResponse = {
    val toQueryMaster = queryMasterExtractor(queryMasterIdTagName, networkQueryIdTagName, nameTagName, userIdTagName, groupIdTagName, createDateTagName, heldTagName, flaggedTagName, flagMessageTagName)
    
    val queryMasterAttempts = xml.map(toQueryMaster)

    //NB: Preserve old exception-throwing behavior for now
    val queryMasters = Tries.sequence(queryMasterAttempts).get

    ReadPreviousQueriesResponse(queryMasters)
  }
  
  import NodeSeqEnrichments.Strictness._
  
  private[this] val toText = (_: NodeSeq).text.trim
  private[this] val toLong = toText(_: NodeSeq).toLong

  private[this] def queryMasterExtractor(queryMasterIdTagName: String, networkQueryIdTagName: String, nameTagName: String, userIdTagName: String, groupIdTagName: String, createDateTagName: String, heldTagName: String, flaggedTagName: String, flagMessageTagName: String): NodeSeq => Try[QueryMaster] = {
    (querymasterXml: NodeSeq) => {
      for {
        queryMasterId <- querymasterXml.withChild(queryMasterIdTagName).map(toText)
        networkQueryId <- querymasterXml.withChild(networkQueryIdTagName).map(toLong)
        name <- querymasterXml.withChild(nameTagName).map(toText)
        userId <- querymasterXml.withChild(userIdTagName).map(toText)
        groupId <- querymasterXml.withChild(groupIdTagName).map(toText)
        createDateText <- querymasterXml.withChild(createDateTagName).map(toText)
        createDate <- XmlDateHelper.parseXmlTime(createDateText)
        flagged = (querymasterXml \ flaggedTagName).headOption.map(toText(_).toBoolean)
        flagMessage = (querymasterXml \ flagMessageTagName).headOption.map(toText(_))
      } yield {
        QueryMaster(queryMasterId, networkQueryId, name, userId, groupId, createDate, flagged, flagMessage)
      }
    }
  }
}