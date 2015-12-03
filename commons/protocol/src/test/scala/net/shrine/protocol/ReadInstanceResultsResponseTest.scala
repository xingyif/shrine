package net.shrine.protocol

import org.junit.Test
import junit.framework.TestCase
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlUtil
import scala.xml.NodeSeq

/**
 * @author Bill Simons
 * @date 4/14/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class ReadInstanceResultsResponseTest extends TestCase with ShrineResponseI2b2SerializableValidator {
  val shrineNetworkQueryId = 1111111L
  val resultId1 = 1111111L
  val setSize = 12
  val type1 = ResultOutputType.PATIENTSET
  val statusName1 = QueryResult.StatusType.Finished
  val startDate1 = XmlDateHelper.now
  val endDate1 = XmlDateHelper.now
  val result1 = QueryResult(resultId1, shrineNetworkQueryId, Option(type1), setSize, Option(startDate1), Option(endDate1),None, statusName1, Option(statusName1.name))

  val resultId2 = 222222L
  val type2 = ResultOutputType.PATIENT_COUNT_XML
  val statusName2 = QueryResult.StatusType.Finished
  val startDate2 = XmlDateHelper.now
  val endDate2 = XmlDateHelper.now
  val result2 = QueryResult(resultId2, shrineNetworkQueryId, Option(type2), setSize, Option(startDate2), Option(endDate2), None, statusName2, Option(statusName2.name))

  override def messageBody: NodeSeq = {
    <message_body>
      <ns5:response xmlns="" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:result_responseType">
        <status>
          <condition type="DONE">DONE</condition>
        </status>
        <query_result_instance>
          <result_instance_id>{ resultId1 }</result_instance_id>
          <query_instance_id>{ shrineNetworkQueryId }</query_instance_id>
          { type1.toI2b2 }
          <set_size>{ setSize }</set_size>
          <start_date>{ startDate1 }</start_date>
          <end_date>{ endDate1 }</end_date>
          <query_status_type>
            <name>{ statusName1 }</name>
            <status_type_id>3</status_type_id>
            <description>FINISHED</description>
          </query_status_type>
        </query_result_instance>
      </ns5:response>
    </message_body>
  }

  private val readInstanceResultsResponse = XmlUtil.stripWhitespace {
    <readInstanceResultsResponse>
      <shrineNetworkQueryId>{ shrineNetworkQueryId }</shrineNetworkQueryId>
      <queryResults>
        <queryResult>
          <resultId>{ resultId1 }</resultId>
          <instanceId>{ shrineNetworkQueryId }</instanceId>
          { type1.toXml }
          <setSize>{ setSize }</setSize>
          <startDate>{ startDate1 }</startDate>
          <endDate>{ endDate1 }</endDate>
          <status>{ statusName1 }</status>
          <statusMessage>{ statusName1.name }</statusMessage>
        </queryResult>
      </queryResults>
    </readInstanceResultsResponse>
  }

  import DefaultBreakdownResultOutputTypes.{ values => breakdownTypes }
  
  @Test
  def testFromXml {
    val actual = ReadInstanceResultsResponse.fromXml(breakdownTypes.toSet)(readInstanceResultsResponse)

    actual.shrineNetworkQueryId should equal(shrineNetworkQueryId)

    actual.singleNodeResult should equal(result1)
  }

  @Test
  def testToXml {
    //we compare the string versions of the xml because Scala's xml equality does not always behave properly
    new ReadInstanceResultsResponse(shrineNetworkQueryId, result1).toXmlString should equal(readInstanceResultsResponse.toString)
  }

  @Test
  def testFromI2b2 {
    val actual = ReadInstanceResultsResponse.fromI2b2(breakdownTypes.toSet)(response)

    actual.shrineNetworkQueryId should equal(shrineNetworkQueryId)
    actual.singleNodeResult should equal(result1)
  }

  @Test
  def testToI2b2 {
    //we compare the string versions of the xml because Scala's xml equality does not always behave properly
    val actual = new ReadInstanceResultsResponse(shrineNetworkQueryId, result1).toI2b2String

    actual should equal(response.toString)
  }

  @Test
  def testResults {
    ReadInstanceResultsResponse(shrineNetworkQueryId, result2).results should equal(Seq(result2))
  }
}