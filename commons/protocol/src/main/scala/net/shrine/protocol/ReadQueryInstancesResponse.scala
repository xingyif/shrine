package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.{ I2b2Unmarshaller, XmlUnmarshaller }
import net.shrine.util.XmlDateHelper

/**
 * @author Bill Simons
 * @date 4/13/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class ReadQueryInstancesResponse(
  val queryMasterId: Long,
  val userId: String,
  val groupId: String,
  val queryInstances: Seq[QueryInstance]) extends ShrineResponse {

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
              <end_date>{ queryInstance.endDate }</end_date>
              <query_status_type>
                <status_type_id>6</status_type_id>
                <name>COMPLETED</name>
                <description>COMPLETED</description>
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
              <endDate>{ queryInstance.endDate }</endDate>
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
      val endDate = XmlDateHelper.parseXmlTime((x \ "end_date").text).get //NB: Preserve old exception-throwing behavior for now

      QueryInstance(queryInstanceId, queryMasterId, userId, groupId, startDate, endDate)
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
      val endDate = XmlDateHelper.parseXmlTime((x \ "endDate").text).get //NB: Preserve old exception-throwing behavior for now

      QueryInstance(queryInstanceId, masterId.toString, userId, groupId, startDate, endDate)
    }

    ReadQueryInstancesResponse(masterId, userId, groupId, queryInstances)
  }
}
