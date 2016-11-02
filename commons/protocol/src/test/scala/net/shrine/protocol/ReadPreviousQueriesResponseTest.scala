package net.shrine.protocol

import org.junit.Test
import net.shrine.util.XmlUtil
import net.shrine.util.XmlDateHelper
import javax.xml.datatype.XMLGregorianCalendar

import net.shrine.problem.TestProblem

/**
 * @author Bill Simons
 * @since 4/12/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class ReadPreviousQueriesResponseTest extends ShrineResponseI2b2SerializableValidator {

  val queryMasterId1 = 1111111L
  val queryMasterId2 = 222222L

  val networkQueryId1 = 123455667L
  val networkQueryId2 = 98327592L
  
  val queryName1 = "name1"
  val queryName2 = "name2"

  val userId1 = Some("user1")
  val userId2 = Some("user2")

  val groupId1 = Some("group1")
  val groupId2 = Some("group2")

  val createDate1 = XmlDateHelper.now
  val createDate2 = XmlDateHelper.now

  val flagged1 = Some(true)
  val flagged2 = None

  val flagMessage1 = Some("askldhlaksdjlkasdjklasdjl")
  val flagMessage2 = None

  val queryMaster1 = makeQueryMaster(queryMasterId1, networkQueryId1, queryName1, userId1, groupId1, createDate1, flagged1, flagMessage1)
  val queryMaster2 = makeQueryMaster(queryMasterId2, networkQueryId2, queryName2, userId2, groupId2, createDate2, flagged2, flagMessage2)

  def makeQueryMaster(queryMasterId: Long, networkQueryId: Long, queryName: String, userId: Option[String], groupId: Option[String], createDate: XMLGregorianCalendar, flagged: Option[Boolean], flagMessage: Option[String]) = {
    QueryMaster(String.valueOf(queryMasterId), networkQueryId, queryName, userId.get, groupId.get, createDate, flagged, flagMessage)
  }

  def messageBody = XmlUtil.stripWhitespace {
    <message_body>
      <ns5:response xmlns="" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:master_responseType">
        <status>
          <condition type="DONE">DONE</condition>
        </status>
        <query_master>
          <query_master_id>{ queryMasterId1 }</query_master_id>
          <network_query_id>{ networkQueryId1 }</network_query_id>
          <name>{ queryName1 }</name>
          <user_id>{ userId1.get }</user_id>
          <group_id>{ groupId1.get }</group_id>
          <create_date>{ createDate1 }</create_date>
          <flagged>{ flagged1.get }</flagged>
          <flagMessage>{ flagMessage1.get }</flagMessage>
        </query_master>
        <query_master>
          <query_master_id>{ queryMasterId2 }</query_master_id>
          <network_query_id>{ networkQueryId2 }</network_query_id>
          <name>{ queryName2 }</name>
          <user_id>{ userId2.get }</user_id>
          <group_id>{ groupId2.get }</group_id>
          <create_date>{ createDate2 }</create_date>
        </query_master>
      </ns5:response>
    </message_body>
  }

  //keep the held field around to be sure that old messages can be read.
  def oldMessageBody = XmlUtil.stripWhitespace {
    <message_body>
      <ns5:response xmlns="" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:master_responseType">
        <status>
          <condition type="DONE">DONE</condition>
        </status>
        <query_master>
          <query_master_id>{ queryMasterId1 }</query_master_id>
          <network_query_id>{ networkQueryId1 }</network_query_id>
          <name>{ queryName1 }</name>
          <user_id>{ userId1.get }</user_id>
          <group_id>{ groupId1.get }</group_id>
          <create_date>{ createDate1 }</create_date>
          <flagged>{ flagged1.get }</flagged>
          <flagMessage>{ flagMessage1.get }</flagMessage>
        </query_master>
        <query_master>
          <query_master_id>{ queryMasterId2 }</query_master_id>
          <network_query_id>{ networkQueryId2 }</network_query_id>
          <name>{ queryName2 }</name>
          <user_id>{ userId2.get }</user_id>
          <group_id>{ groupId2.get }</group_id>
          <create_date>{ createDate2 }</create_date>
          <held>false</held>
        </query_master>
      </ns5:response>
    </message_body>
  }


  val readPreviousQueriesResponse = XmlUtil.stripWhitespace {
    <readPreviousQueriesResponse>
      <queryMaster>
        <masterId>{ queryMasterId1 }</masterId>
        <networkId>{ networkQueryId1 }</networkId>
        <name>{ queryName1 }</name>
        <createDate>{ createDate1 }</createDate>
        <userId>{ userId1.get }</userId>
        <groupId>{ groupId1.get }</groupId>
        <flagged>{ flagged1.get }</flagged>
        <flagMessage>{ flagMessage1.get }</flagMessage>
      </queryMaster>
      <queryMaster>
        <masterId>{ queryMasterId2 }</masterId>
        <networkId>{ networkQueryId2 }</networkId>
        <name>{ queryName2 }</name>
        <createDate>{ createDate2 }</createDate>
        <userId>{ userId2.get }</userId>
        <groupId>{ groupId2.get }</groupId>
      </queryMaster>
    </readPreviousQueriesResponse>
  }

  @Test
  def testFromI2b2FailsFast() {
    intercept[Exception] {
      ReadPreviousQueriesResponse.fromI2b2(<foo/>)
    }

    intercept[Exception] {
      ReadPreviousQueriesResponse.fromI2b2(ErrorResponse(TestProblem("foo!")).toI2b2)
    }
  }

  @Test
  def testFromXml() {
    val actual = ReadPreviousQueriesResponse.fromXml(readPreviousQueriesResponse)

    val expectedQueryMasters = Set(queryMaster1, queryMaster2)

    actual.queryMasters.toSet should equal(expectedQueryMasters)
  }

  @Test
  def testToXml() {
    //we compare the string versions of the xml because Scala's xml equality does not always behave properly
    ReadPreviousQueriesResponse(Seq(queryMaster1, queryMaster2)).toXmlString should equal(readPreviousQueriesResponse.toString)
  }

  @Test
  def testFromI2b2() {
    val translatedResponse = ReadPreviousQueriesResponse.fromI2b2(response)

    translatedResponse.queryMasters.toSet should equal(Set(queryMaster1, queryMaster2))
  }

  @Test
  def testToI2b2() {
    //Per-queryMaster userids and groupids

    //we compare the string versions of the xml because Scala's xml equality does not always behave properly
    val actual = ReadPreviousQueriesResponse(Seq(queryMaster1, queryMaster2)).toI2b2String

    val expected = response.toString

    actual should equal(expected)
  }
}