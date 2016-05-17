package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.util.Try
import scala.xml.NodeSeq

import net.shrine.serialization.I2b2Marshaller
import net.shrine.util.XmlUtil

/**
 * @author Bill Simons
 * @since 3/9/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
abstract class ShrineRequest(
    override val projectId: String, 
    override val waitTime: Duration, 
    override val authn: AuthenticationInfo) extends BaseShrineRequest with HasHeaderFields with I2b2Marshaller {
  
  protected def i2b2MessageBody: NodeSeq
  
  override def toI2b2 = XmlUtil.stripWhitespace {
    <ns6:request xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns8="http://sheriff.shrine.net/" xmlns:ns7="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/hive/plugin/" xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns99="http://www.i2b2.org/xsd/cell/pm/1.1/" xmlns:ns100="http://www.i2b2.org/xsd/cell/ont/1.1/">
      <message_header>
        <proxy>
          <redirect_url>https://localhost/shrine/QueryToolService/request</redirect_url>
        </proxy>
        <sending_application>
          <application_name>i2b2_QueryTool</application_name>
          <application_version>0.2</application_version>
        </sending_application>
        <sending_facility>
          <facility_name>SHRINE</facility_name>
        </sending_facility>
        <receiving_application>
          <application_name>i2b2_DataRepositoryCell</application_name>
          <application_version>0.2</application_version>
        </receiving_application>
        <receiving_facility>
          <facility_name>SHRINE</facility_name>
        </receiving_facility>
        { authn.toI2b2 }
        <message_type>
          <message_code>Q04</message_code>
          <event_type>EQQ</event_type>
        </message_type>
        <message_control_id>
          <message_num>EQ7Szep1Md11K4E7zEc99</message_num>
          <instance_num>0</instance_num>
        </message_control_id>
        <processing_id>
          <processing_id>P</processing_id>
          <processing_mode>I</processing_mode>
        </processing_id>
        <accept_acknowledgement_type>AL</accept_acknowledgement_type>
        <project_id>{ projectId }</project_id>
        <country_code>US</country_code>
      </message_header>
      <request_header>
        <result_waittime_ms>{ waitTime.toMillis }</result_waittime_ms>
      </request_header>
      { i2b2MessageBody }
    </ns6:request>
  }
}

object ShrineRequest {

  type Unmarshaller[R] = Set[ResultOutputType] => NodeSeq => Try[R]
  
  def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ShrineRequest] = {
    for {
      head <- Try(xml.head)
      tagName = head.label
      if shrineUnmarshallers.contains(tagName)
      unmarshal = shrineUnmarshallers(tagName)
      result <- unmarshal(breakdownTypes)(xml)
    } yield result
  }
  
  private val shrineUnmarshallers: Map[String, Unmarshaller[ShrineRequest]] = Map(
    "deleteQuery" -> DeleteQueryRequest.fromXml _,
    "readApprovedQueryTopics" -> ReadApprovedQueryTopicsRequest.fromXml _,
    "readInstanceResults" -> ReadInstanceResultsRequest.fromXml _,
    "readPreviousQueries" -> ReadPreviousQueriesRequest.fromXml _,
    "readQueryDefinition" -> ReadQueryDefinitionRequest.fromXml _,
    "readQueryInstances" -> ReadQueryInstancesRequest.fromXml _,
    "renameQuery" -> RenameQueryRequest.fromXml _,
    "runQuery" -> RunQueryRequest.fromXml _,
    "readResult" -> ReadResultRequest.fromXml _,
    "readAdminPreviousQueries" -> ReadI2b2AdminPreviousQueriesRequest.fromXml _,
    FlagQueryRequest.rootTagName -> FlagQueryRequest.fromXml _,
    UnFlagQueryRequest.rootTagName -> UnFlagQueryRequest.fromXml _)
}