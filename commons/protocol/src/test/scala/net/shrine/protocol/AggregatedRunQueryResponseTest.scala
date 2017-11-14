package net.shrine.protocol

import net.shrine.problem.TestProblem

import scala.xml.NodeSeq
import org.junit.Test
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.protocol.version.v24.AggregatedRunQueryResponse
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlUtil

/**
 *
 *
 * @author Justin Quan
 * @see http://chip.org
 * @since 8/12/11
 */
//noinspection EmptyParenMethodOverridenAsParameterless,EmptyParenMethodAccessedAsParameterless,UnitMethodIsParameterless

final class AggregatedRunQueryResponseTest extends ShrineResponseI2b2SerializableValidator {
  private val queryId = 1L
  private val queryName = "queryName"
  private val userId = "user"
  private val groupId = "group"
  private val createDate = XmlDateHelper.now
  private val requestQueryDef = QueryDefinition(queryName, Term("""\\i2b2\i2b2\Demographics\Age\0-9 years old\"""))
  private val queryInstanceId = 2L
  private val resultId = 3L
  private val setSize = 10L
  private val startDate = createDate
  private val endDate = createDate
  private val resultId2 = 4L
  private val resultType1 = ResultOutputType.PATIENT_COUNT_XML
  private val resultType2 = ResultOutputType.PATIENT_COUNT_XML
  private val statusType = QueryResult.StatusType.Finished

  override def messageBody = {
    <message_body>
      <ns5:response xmlns="" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:master_instance_result_responseType">
        <status>
          <condition type="DONE">DONE</condition>
        </status>
        <query_master>
          <query_master_id>{ queryId }</query_master_id>
          <name>{ queryName }</name>
          <user_id>{ userId }</user_id>
          <group_id>{ groupId }</group_id>
          <create_date>{ createDate }</create_date>
          <request_xml>{ requestQueryDef.toI2b2 }</request_xml>
        </query_master>
        <query_instance>
          <query_instance_id>{ queryInstanceId }</query_instance_id>
          <query_master_id>{ queryId }</query_master_id>
          <user_id>{ userId }</user_id>
          <group_id>{ groupId }</group_id>
          <query_status_type>
            <status_type_id>6</status_type_id>
            <name>COMPLETED</name>
            <description>COMPLETED</description>
          </query_status_type>
        </query_instance>
        <query_result_instance>
          <result_instance_id>{ resultId }</result_instance_id>
          <query_instance_id>{ queryInstanceId }</query_instance_id>
          <query_result_type>
						<result_type_id>4</result_type_id>
            <name>{ resultType1 }</name>
            <display_type>CATNUM</display_type>
            <visual_attribute_type>LA</visual_attribute_type>
            <description>Number of patients</description>
          </query_result_type>
          <set_size>{ setSize }</set_size>
          <start_date>{ startDate }</start_date>
          <end_date>{ endDate }</end_date>
          <query_status_type>
            <name>{ statusType }</name>
            <status_type_id>3</status_type_id>
            <description>FINISHED</description>
          </query_status_type>
        </query_result_instance>
        <query_result_instance>
          <result_instance_id>{ resultId2 }</result_instance_id>
          <query_instance_id>{ queryInstanceId }</query_instance_id>
          <query_result_type>
						<result_type_id>4</result_type_id>
            <name>{ resultType2 }</name>
            <display_type>CATNUM</display_type>
            <visual_attribute_type>LA</visual_attribute_type>
            <description>Number of patients</description>
          </query_result_type>
          <set_size>{ setSize }</set_size>
          <start_date>{ startDate }</start_date>
          <end_date>{ endDate }</end_date>
          <query_status_type>
            <name>{ statusType }</name>
            <status_type_id>3</status_type_id>
            <description>FINISHED</description>
          </query_status_type>
        </query_result_instance>
      </ns5:response>
    </message_body>
  }

  private val qr1 = QueryResult(
    resultId = resultId,
    instanceId = queryInstanceId,
    resultType = Option(resultType1),
    setSize = setSize,
    startDate = Option(createDate),
    endDate = Option(createDate),
    description = None,
    statusType = statusType,
    statusMessage = Some(statusType.name),
    problemDigest = None,
    breakdowns = Map.empty)
  private val qr2 = QueryResult(
    resultId = resultId2,
    instanceId = queryInstanceId,
    resultType = Option(resultType2),
    setSize = setSize,
    startDate = Option(createDate),
    endDate = Option(createDate),
    description = None,
    statusType = statusType,
    statusMessage = Some(statusType.name),
    problemDigest = None,
    breakdowns = Map.empty)

  private val runQueryResponse = XmlUtil.stripWhitespace {
    <aggregatedRunQueryResponse>
      <queryId>{ queryId }</queryId>
      <instanceId>{ queryInstanceId }</instanceId>
      <userId>{ userId }</userId>
      <groupId>{ groupId }</groupId>
      <requestXml>{ requestQueryDef.toXml }</requestXml>
      <createDate>{ createDate }</createDate>
      <queryResults>
        {
          Seq(qr1, qr2).map(_.toXml)
        }
      </queryResults>
    </aggregatedRunQueryResponse>
  }

  import DefaultBreakdownResultOutputTypes.{ values => breakdownTypes }

  @Test
  def testFromXml {
    val actual = AggregatedRunQueryResponse.fromXml(breakdownTypes.toSet)(runQueryResponse).get

    actual.queryId should equal(queryId)
    actual.createDate should equal(createDate)
    actual.userId should equal(userId)
    actual.groupId should equal(groupId)
    actual.requestXml should equal(requestQueryDef)
    actual.queryInstanceId should equal(queryInstanceId)
    actual.results should equal(Seq(qr1, qr2))
    actual.queryName should equal(queryName)
  }

  @Test
  def testToXml {
    AggregatedRunQueryResponse(queryId, createDate, userId, groupId, requestQueryDef, queryInstanceId, Seq(qr1, qr2)).toXmlString should equal(runQueryResponse.toString)
  }

  @Test
  def testFromI2b2 {
    val translatedResponse = AggregatedRunQueryResponse.fromI2b2(breakdownTypes.toSet)(response).get

    translatedResponse.queryId should equal(queryId)
    translatedResponse.createDate should equal(createDate)
    translatedResponse.userId should equal(userId)
    translatedResponse.groupId should equal(groupId)
    translatedResponse.requestXml should equal(requestQueryDef)
    translatedResponse.queryInstanceId should equal(queryInstanceId)
    translatedResponse.results should equal(Seq(qr1, qr2))
    translatedResponse.queryName should equal(queryName)
  }

  @Test
  def testFromI2b2StringRequestXml {
    val hackToProduceXml = new HasResponse {
      //Produces a message body where the <request_xml> tag contains escaped XML as a String, as is produced by the CRC
      override def messageBody: NodeSeq = {
        <message_body>
          <ns5:response xmlns="" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:master_instance_result_responseType">
            <status>
              <condition type="DONE">DONE</condition>
            </status>
            <query_master>
              <query_master_id>{ queryId }</query_master_id>
              <name>{ queryName }</name>
              <user_id>{ userId }</user_id>
              <group_id>{ groupId }</group_id>
              <create_date>{ createDate }</create_date>
              <!-- Because requestQueryDef is turned to a String, it will be escaped in the XML, -->
              <!-- the handling of which is what we want to test. -->
              <request_xml>{ requestQueryDef.toI2b2String }</request_xml>
            </query_master>
            <query_instance>
              <query_instance_id>{ queryInstanceId }</query_instance_id>
              <query_master_id>{ queryId }</query_master_id>
              <user_id>{ userId }</user_id>
              <group_id>{ groupId }</group_id>
              <query_status_type>
                <status_type_id>6</status_type_id>
                <name>COMPLETED</name>
                <description>COMPLETED</description>
              </query_status_type>
            </query_instance>
            <query_result_instance>
              <result_instance_id>{ resultId }</result_instance_id>
              <query_instance_id>{ queryInstanceId }</query_instance_id>
              <query_result_type>
                <name>{ resultType1 }</name>
                <result_type_id>1</result_type_id>
                <display_type>LIST</display_type>
                <visual_attribute_type>LA</visual_attribute_type>
                <description>Patient list</description>
              </query_result_type>
              <set_size>{ setSize }</set_size>
              <start_date>{ startDate }</start_date>
              <end_date>{ endDate }</end_date>
              <query_status_type>
                <name>{ statusType }</name>
                <status_type_id>3</status_type_id>
                <description>FINISHED</description>
              </query_status_type>
            </query_result_instance>
            <query_result_instance>
              <result_instance_id>{ resultId2 }</result_instance_id>
              <query_instance_id>{ queryInstanceId }</query_instance_id>
              <query_result_type>
                <name>{ resultType2 }</name>
                <result_type_id>4</result_type_id>
                <display_type>CATNUM</display_type>
                <visual_attribute_type>LA</visual_attribute_type>
                <description>Number of patients</description>
              </query_result_type>
              <set_size>{ setSize }</set_size>
              <start_date>{ startDate }</start_date>
              <end_date>{ endDate }</end_date>
              <query_status_type>
                <name>{ statusType }</name>
                <status_type_id>3</status_type_id>
                <description>FINISHED</description>
              </query_status_type>
            </query_result_instance>
          </ns5:response>
        </message_body>
      }
    }

    doTestFromI2b2(hackToProduceXml.response, requestQueryDef)
  }

  private def doTestFromI2b2(i2b2Response: NodeSeq, expectedQueryDef: AnyRef) {
    val translatedResponse = AggregatedRunQueryResponse.fromI2b2(breakdownTypes.toSet)(i2b2Response).get

    translatedResponse.queryId should equal(queryId)
    translatedResponse.createDate should equal(createDate)
    translatedResponse.userId should equal(userId)
    translatedResponse.groupId should equal(groupId)
    translatedResponse.requestXml should equal(expectedQueryDef)
    translatedResponse.queryInstanceId should equal(queryInstanceId)
    translatedResponse.results should equal(Seq(qr1, qr2))
    translatedResponse.queryName should equal(queryName)
  }

  @Test
  def testResultsPartitioned {
    val actual = AggregatedRunQueryResponse.fromXml(breakdownTypes.toSet)(runQueryResponse).get

    {
      val (nonErrors, errors) = actual.resultsPartitioned

      nonErrors should equal(Seq(qr1, qr2))
      errors.size should equal(0)
    }

    val error = QueryResult.errorResult(None, "something broke", TestProblem())

    {
      val withOnlyErrors = actual.withResults(Seq(error, error))

      val (nonErrors, errors) = withOnlyErrors.resultsPartitioned

      nonErrors.size should equal(0)
      errors should equal(Seq(error, error))
    }

    {
      val withErrorsAndSuccesses = actual.withResults(actual.results ++ Seq(error, error))

      val (nonErrors, errors) = withErrorsAndSuccesses.resultsPartitioned

      nonErrors should equal(Seq(qr1, qr2))
      errors should equal(Seq(error, error))
    }

    {
      val withNoResults = actual.withResults(Nil)

      val (nonErrors, errors) = withNoResults.resultsPartitioned

      nonErrors.size should equal(0)
      errors.size should equal(0)
    }
  }

  @Test
  def testToI2b2 {
    AggregatedRunQueryResponse(queryId, createDate, userId, groupId, requestQueryDef, queryInstanceId, Seq(qr1, qr2)).toI2b2String should equal(response.toString)
  }
}