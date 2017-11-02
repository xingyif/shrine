package net.shrine.protocol

import org.junit.Test

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlUtil

/**
 * @author Bill Simons
 * @since 4/13/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class ReadQueryInstancesResponseTest extends ShrineResponseI2b2SerializableValidator {
  val queryMasterId = 1111111L
  val queryInstanceId1 = 1111111L
  val userId = "user1"
  val groupId = "group1"
  val startDate1 = XmlDateHelper.now
  val endDate1 = XmlDateHelper.now
  val queryInstance1 = makeQueryInstance(queryMasterId, queryInstanceId1, userId, groupId, startDate1, endDate1)

  val queryInstanceId2 = 222222L
  val startDate2 = XmlDateHelper.now
  val endDate2 = XmlDateHelper.now
  val queryInstance2 = makeQueryInstance(queryMasterId, queryInstanceId2, userId, groupId, startDate2, endDate2)

  def makeQueryInstance(queryMasterId: Long, queryInstanceId: Long, userId: String, groupId: String, startDate: XMLGregorianCalendar, endDate: XMLGregorianCalendar) = {

    QueryInstance(String.valueOf(queryInstanceId), String.valueOf(queryMasterId), userId, groupId, startDate, endDate)
  }

  override def messageBody = {
    <message_body>
      <ns5:response xmlns="" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:instance_responseType">
        <status>
          <condition type="DONE">DONE</condition>
        </status>
        <query_instance>
          <query_instance_id>{ queryInstanceId1 }</query_instance_id>
          <query_master_id>{ queryMasterId }</query_master_id>
          <user_id>{ userId }</user_id>
          <group_id>{ groupId }</group_id>
          <start_date>{ startDate1 }</start_date>
          <end_date>{ endDate1 }</end_date>
          <query_status_type>
            <status_type_id>6</status_type_id>
            <name>COMPLETED</name>
            <description>COMPLETED</description>
          </query_status_type>
        </query_instance>
        <query_instance>
          <query_instance_id>{ queryInstanceId2 }</query_instance_id>
          <query_master_id>{ queryMasterId }</query_master_id>
          <user_id>{ userId }</user_id>
          <group_id>{ groupId }</group_id>
          <start_date>{ startDate2 }</start_date>
          <end_date>{ endDate2 }</end_date>
          <query_status_type>
            <status_type_id>6</status_type_id>
            <name>COMPLETED</name>
            <description>COMPLETED</description>
          </query_status_type>
        </query_instance>
      </ns5:response>
    </message_body>
  }

  val readQueryInstancesResponse = XmlUtil.stripWhitespace {
    <readQueryInstancesResponse>
      <masterId>{ queryMasterId }</masterId>
      <userId>{ userId }</userId>
      <groupId>{ groupId }</groupId>
      <queryInstance>
        <instanceId>{ queryInstanceId1 }</instanceId>
        <startDate>{ startDate1 }</startDate>
        <endDate>{ endDate1 }</endDate>
      </queryInstance>
      <queryInstance>
        <instanceId>{ queryInstanceId2 }</instanceId>
        <startDate>{ startDate2 }</startDate>
        <endDate>{ endDate2 }</endDate>
      </queryInstance>
    </readQueryInstancesResponse>
  }

  val readQueryInstanceResponseI2b2XmlMessageBodyForINCOMPLETE = XmlUtil.loadStringIgnoringRemoteResources(
    """<message_body>
      |        <ns4:response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:instance_responseType">
      |            <status>
      |                <condition type="DONE">DONE</condition>
      |            </status>
      |            <query_instance>
      |                <query_instance_id>53</query_instance_id>
      |                <query_master_id>53</query_master_id>
      |                <user_id>demo</user_id>
      |                <group_id>Demo</group_id>
      |                <batch_mode>MEDIUM_QUEUE_RUNNING</batch_mode>
      |                <start_date>2017-10-09T22:08:18.471-04:00</start_date>
      |                <message/>
      |                <query_status_type>
      |                    <status_type_id>5</status_type_id>
      |                    <name>INCOMPLETE</name>
      |                    <description>INCOMPLETE</description>
      |                </query_status_type>
      |            </query_instance>
      |        </ns4:response>
      |    </message_body>""".stripMargin).get

  @Test
  def testIncompleteFromI2b2():Unit = {
    val actual = ReadQueryInstancesResponse.fromI2b2(response(readQueryInstanceResponseI2b2XmlMessageBodyForINCOMPLETE))

    actual.queryMasterId should equal(53)
    actual.userId should equal("demo")
    actual.groupId should equal("Demo")
  }

  @Test
  override def testFromXml() {
    val actual = ReadQueryInstancesResponse.fromXml(readQueryInstancesResponse)
    actual.queryMasterId should equal(queryMasterId)
    actual.userId should equal(userId)
    actual.groupId should equal(groupId)
    actual.queryInstances.contains(queryInstance1) should be(true)
    actual.queryInstances.contains(queryInstance2) should be(true)
  }

  @Test
  override def testToXml() {
    //we compare the string versions of the xml because Scala's xml equality does not always behave properly
    new ReadQueryInstancesResponse(queryMasterId, userId, groupId, Seq(queryInstance1, queryInstance2)).toXml.toString should equal(readQueryInstancesResponse.toString)
  }

  @Test
  override def testFromI2b2() {
    val actual = ReadQueryInstancesResponse.fromI2b2(response)
    actual.queryMasterId should equal(queryMasterId)
    actual.userId should equal(userId)
    actual.groupId should equal(groupId)
    actual.queryInstances.contains(queryInstance1) should be(true)
    actual.queryInstances.contains(queryInstance2) should be(true)
  }

  @Test
  override def testToI2b2() {
    //we compare the string versions of the xml because Scala's xml equality does not always behave properly
    new ReadQueryInstancesResponse(queryMasterId, userId, groupId, Seq(queryInstance1, queryInstance2)).toI2b2.toString should equal(response.toString)
  }
}