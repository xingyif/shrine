package net.shrine.protocol

import junit.framework.TestCase
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlUtil
import org.junit.Test
import scala.xml.NodeSeq

/**
 * @author clint
 * @author Bill Simons
 * @since Dec 4, 2012
 */
//noinspection EmptyParenMethodOverridenAsParameterless,UnitMethodIsParameterless
final class AggregatedReadInstanceResultsResponseTest extends TestCase with ShrineResponseI2b2SerializableValidator {
  val shrineNetworkQueryId = 1111111L
  val resultId1 = 1111111L
  val setSize = 12
  val type1 = ResultOutputType.PATIENT_COUNT_XML
  val statusType1 = QueryResult.StatusType.Finished
  val startDate1 = XmlDateHelper.now
  val endDate1 = XmlDateHelper.now
  val result1 = QueryResult(
    resultId = resultId1,
    instanceId = shrineNetworkQueryId,
    resultType = Option(type1),
    setSize = setSize,
    startDate = Option(startDate1),
    endDate = Option(endDate1),
    description = None,
    statusType = statusType1,
    statusMessage = Option(statusType1.name)
  )

  val resultId2 = 222222L
  val type2 = ResultOutputType.PATIENT_COUNT_XML
  val statusType2 = QueryResult.StatusType.Finished
  val startDate2 = XmlDateHelper.now
  val endDate2 = XmlDateHelper.now
  val result2 = QueryResult(resultId2, shrineNetworkQueryId, Option(type2), setSize, Option(startDate2), Option(endDate2), None, statusType2, Option(statusType2.name))

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
            <name>{ statusType1.name }</name>
            <status_type_id>3</status_type_id>
            <description>FINISHED</description>
          </query_status_type>
        </query_result_instance>
        <query_result_instance>
          <result_instance_id>{ resultId2 }</result_instance_id>
          <query_instance_id>{ shrineNetworkQueryId }</query_instance_id>
          { type2.toI2b2 }
          <set_size>{ setSize }</set_size>
          <start_date>{ startDate2 }</start_date>
          <end_date>{ endDate2 }</end_date>
          <query_status_type>
            <name>{ statusType2.name }</name>
            <status_type_id>3</status_type_id>
            <description>FINISHED</description>
          </query_status_type>
        </query_result_instance>
      </ns5:response>
    </message_body>
  }

  private val readInstanceResultsResponse = XmlUtil.stripWhitespace {
    <aggregatedReadInstanceResultsResponse>
      <shrineNetworkQueryId>{ shrineNetworkQueryId }</shrineNetworkQueryId>
      <queryResults>
        <queryResult>
          <resultId>{ resultId1 }</resultId>
          <instanceId>{ shrineNetworkQueryId }</instanceId>
          { type1.toXml }
          <setSize>{ setSize }</setSize>
          <startDate>{ startDate1 }</startDate>
          <endDate>{ endDate1 }</endDate>
          <status>{ statusType1 }</status>
          <statusMessage> { statusType1.name }</statusMessage> 
        </queryResult>
        <queryResult>
          <resultId>{ resultId2 }</resultId>
          <instanceId>{ shrineNetworkQueryId }</instanceId>
          { type2.toXml }
          <setSize>{ setSize }</setSize>
          <startDate>{ startDate2 }</startDate>
          <endDate>{ endDate2 }</endDate>
          <status>{ statusType2 }</status>
          <statusMessage> { statusType2.name }</statusMessage>
        </queryResult>
      </queryResults>
    </aggregatedReadInstanceResultsResponse>
  }

  import DefaultBreakdownResultOutputTypes.{values => breakdownTypes}
  
  @Test
  def testFromXml {
    
    val fromXml = AggregatedReadInstanceResultsResponse.fromXml(breakdownTypes.toSet)(readInstanceResultsResponse)

    fromXml.shrineNetworkQueryId should equal(shrineNetworkQueryId)

    fromXml.results should equal(Seq(result1, result2))
  }

  @Test
  def testResults {
    AggregatedReadInstanceResultsResponse(shrineNetworkQueryId, Seq(result1, result2)).results should equal(Seq(result1, result2))
  }

  @Test
  def testToXml {
    val xml = AggregatedReadInstanceResultsResponse(shrineNetworkQueryId, Seq(result1, result2)).toXmlString
    //we compare the string versions of the xml because Scala's xml equality does not always behave properly
   xml should equal(readInstanceResultsResponse.toString())
  }

  @Test
  def testFromI2b2 {
    val actual = AggregatedReadInstanceResultsResponse.fromI2b2(breakdownTypes.toSet)(response)

    actual.shrineNetworkQueryId should equal(shrineNetworkQueryId)

    actual.results should equal(Seq(result1, result2))
  }

  @Test
  def testToI2b2 {
    //we compare the string versions of the xml because Scala's xml equality does not always behave properly
    val actual = AggregatedReadInstanceResultsResponse(shrineNetworkQueryId, Seq(result1, result2)).toI2b2String

    actual should equal(response.toString())
  }
}