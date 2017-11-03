package net.shrine.protocol

import javax.xml.datatype.XMLGregorianCalendar

import net.shrine.protocol.QueryResult.StatusType

import xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.{I2b2Unmarshaller, XmlUnmarshaller}
import net.shrine.util.XmlDateHelper
import net.shrine.util.OptionEnrichments.OptionHasToXml

/**
 * @author Bill Simons
 * @since 4/13/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class ReadQueryInstancesResponse(
                                             queryMasterId: Long,
                                             userId: String,
                                             groupId: String,
                                             queryInstances: Seq[QueryInstance]
                                           ) extends ShrineResponse {

  override protected def i2b2MessageBody = XmlUtil.stripWhitespace {
    <ns5:response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:instance_responseType">
      <status>
        <condition type="DONE">DONE</condition>
      </status>
      {
        queryInstances.map { queryInstance =>
          XmlUtil.stripWhitespace {
            <query_instance>
              <query_instance_id>{ queryInstance.queryInstanceId }</query_instance_id>
              <query_master_id>{ queryMasterId }</query_master_id>
              <user_id>{ userId }</user_id>
              <group_id>{ groupId }</group_id>
              <start_date>{ queryInstance.startDate }</start_date>
              {queryInstance.endDate.toXml(<end_date/>)}
              <query_status_type>
                <status_type_id>{queryInstance.queryStatus.i2b2Id.get}</status_type_id>
                <name>{queryInstance.queryStatus.name}</name>
                <description>{queryInstance.queryStatus.name}</description>
              </query_status_type>
            </query_instance>
          }
        }
      }
    </ns5:response>
  }

  override def toXml = XmlUtil.stripWhitespace {
    <readQueryInstancesResponse>
      <masterId>{ queryMasterId }</masterId>
      <userId>{ userId }</userId>
      <groupId>{ groupId }</groupId>
      {
        queryInstances.map { queryInstance =>
          XmlUtil.stripWhitespace {
            <queryInstance>
              <instanceId>{ queryInstance.queryInstanceId }</instanceId>
              <startDate>{ queryInstance.startDate }</startDate>
              {queryInstance.endDate.toXml(<endDate/>)}
              <queryStatusType>{ queryInstance.queryStatus.name }</queryStatusType>
            </queryInstance>
          }
        }
      }
    </readQueryInstancesResponse>
  }

  def withId(id: Long): ReadQueryInstancesResponse = this.copy(queryMasterId = id)

  def withInstances(newInstances: Seq[QueryInstance]): ReadQueryInstancesResponse = this.copy(queryInstances = newInstances)
}

object ReadQueryInstancesResponse extends I2b2Unmarshaller[ReadQueryInstancesResponse] with XmlUnmarshaller[ReadQueryInstancesResponse] {
  override def fromI2b2(nodeSeq: NodeSeq): ReadQueryInstancesResponse = {
    val queryInstances = (nodeSeq \ "message_body" \ "response" \ "query_instance").map { x =>
      val queryInstanceId = (x \ "query_instance_id").text
      val queryMasterId = (x \ "query_master_id").text
      val userId = (x \ "user_id").text
      val groupId = (x \ "group_id").text
      val startDate = XmlDateHelper.parseXmlTime((x \ "start_date").text).get //NB: Preserve old exception-throwing behavior for now
      val endDate = extractDate(x,"end_date") //NB: Preserve old exception-throwing behavior for now
      val queryStatus: StatusType = QueryResult.StatusType.valueOf(asText("query_status_type", "name")(x)).get //TODO: Avoid fragile .get call
      QueryInstance(queryInstanceId, queryMasterId, userId, groupId, startDate, endDate, queryStatus)
    }

    val firstInstance = queryInstances.head //TODO - parsing error if no masters - need to deal with "no result" cases

    ReadQueryInstancesResponse(firstInstance.queryMasterId.toLong, firstInstance.userId, firstInstance.groupId, queryInstances)
  }

  override def fromXml(nodeSeq: NodeSeq): ReadQueryInstancesResponse = {
    val masterId = (nodeSeq \ "masterId").text.toLong
    val userId = (nodeSeq \ "userId").text
    val groupId = (nodeSeq \ "groupId").text

    val queryInstances = (nodeSeq \ "queryInstance").map { x =>
      val queryInstanceId = (x \ "instanceId").text
      val startDate = XmlDateHelper.parseXmlTime((x \ "startDate").text).get //NB: Preserve old exception-throwing behavior for now
      val endDate = extractDate(x,"endDate") //NB: Preserve old exception-throwing behavior for now
      val statusType = QueryResult.StatusType.valueOf(asText("queryStatusType")(x)).get //TODO: Avoid fragile .get call

      QueryInstance(queryInstanceId, masterId.toString, userId, groupId, startDate, endDate, statusType)
    }

    ReadQueryInstancesResponse(masterId, userId, groupId, queryInstances)
  }

  def extractDate(xml: NodeSeq,elemName: String): Option[XMLGregorianCalendar] = extract(xml,elemName).map(XmlDateHelper.parseXmlTime).map(_.get)
  def extract(xml: NodeSeq,elemName: String): Option[String] = { Option((xml \ elemName).text.trim).filter(!_.isEmpty) }

  def elemAt(path: String*)(xml: NodeSeq): NodeSeq = path.foldLeft(xml)(_ \ _)
  def asText(path: String*)(xml: NodeSeq): String = elemAt(path: _*)(xml).text.trim

}
