package net.shrine.protocol

import net.shrine.problem.ProblemDigest
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.XmlUtil
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlGcEnrichments

/**
 * @author Bill Simons
 * @author clint
 * @since 8/19/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
//noinspection EmptyParenMethodAccessedAsParameterless,NameBooleanParameters
final class QueryResultTest extends ShouldMatchersForJUnit with XmlRoundTripper[QueryResult] with I2b2SerializableValidator {
  private val date = XmlDateHelper.now
  private val resultId = 1L
  private val instanceId = 2L
  private val resultType = ResultOutputType.PATIENTSET
  private val setSize = 12L
  private val statusType = QueryResult.StatusType.Finished
  private val description = "description"
  private val statusMessage = "lakjdalsjd"
  private val problemCodec = "problem.codec"
  private val problemSummary = "test problem"
  private val problemDescription = "problem for testing"
  private val problemDetails =
    """Details of the problem
      |sometimes take
      |multiple lines.
    """.stripMargin

  private val queryResult = QueryResult(resultId, instanceId, Some(resultType), setSize, Option(date), Option(date), Option(description), statusType, Option(statusType.name))

  import DefaultBreakdownResultOutputTypes.{ values => breakdownTypes, _ }

  private val resultWithBreakDowns = queryResult.copy(
    statusMessage = Some(statusMessage),
    breakdowns =
      Map(PATIENT_AGE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_AGE_COUNT_XML, Map("foo" -> 1L, "bar" -> 2L)),
        PATIENT_RACE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_RACE_COUNT_XML, Map("nuh" -> 3L, "zuh" -> 4L)),
        PATIENT_VITALSTATUS_COUNT_XML -> I2b2ResultEnvelope(PATIENT_VITALSTATUS_COUNT_XML, Map("blarg" -> 5L, "glarg" -> 6L)),
        PATIENT_GENDER_COUNT_XML -> I2b2ResultEnvelope(PATIENT_GENDER_COUNT_XML, Map("huh" -> 7L, "yeah" -> 8))))

  private val expectedWhenBreakdownsArePresent = XmlUtil.stripWhitespace {
    <queryResult>
      <resultId>{ resultId }</resultId>
      <instanceId>{ instanceId }</instanceId>
      { resultType.toXml }
      <setSize>{ setSize }</setSize>
      <startDate>{ date }</startDate>
      <endDate>{ date }</endDate>
      <description>{ description }</description>
      <status>{ statusType }</status>
      <statusMessage>{ statusMessage }</statusMessage>
      <resultEnvelope>
        <resultType>{ PATIENT_AGE_COUNT_XML }</resultType>
        <column>
          <name>bar</name>
          <value>2</value>
        </column>
        <column>
          <name>foo</name>
          <value>1</value>
        </column>
      </resultEnvelope>
      <resultEnvelope>
        <resultType>{ PATIENT_GENDER_COUNT_XML }</resultType>
        <column>
          <name>huh</name>
          <value>7</value>
        </column>
        <column>
          <name>yeah</name>
          <value>8</value>
        </column>
      </resultEnvelope>
      <resultEnvelope>
        <resultType>{ PATIENT_RACE_COUNT_XML }</resultType>
        <column>
          <name>nuh</name>
          <value>3</value>
        </column>
        <column>
          <name>zuh</name>
          <value>4</value>
        </column>
      </resultEnvelope>
      <resultEnvelope>
        <resultType>{ PATIENT_VITALSTATUS_COUNT_XML }</resultType>
        <column>
          <name>blarg</name>
          <value>5</value>
        </column>
        <column>
          <name>glarg</name>
          <value>6</value>
        </column>
      </resultEnvelope>
    </queryResult>
  }.toString

  private val expectedI2b2Xml = XmlUtil.stripWhitespace {
    <query_result_instance>
      <result_instance_id>{ resultId }</result_instance_id>
      <query_instance_id>{ instanceId }</query_instance_id>
      <description>{ description }</description>
      <query_result_type>
        <result_type_id>1</result_type_id>
        <name>{ resultType }</name>
        <display_type>LIST</display_type><visual_attribute_type>LA</visual_attribute_type><description>Patient set</description>
      </query_result_type>
      <set_size>{ setSize }</set_size>
      <start_date>{ date }</start_date>
      <end_date>{ date }</end_date>
      <query_status_type>
        <name>{ statusType }</name>
        <status_type_id>3</status_type_id><description>FINISHED</description>
      </query_status_type>
    </query_result_instance>
  }.toString

  private val expectedI2b2XmlWithBreakdowns = XmlUtil.stripWhitespace {
    <query_result_instance>
      <result_instance_id>{ resultId }</result_instance_id>
      <query_instance_id>{ instanceId }</query_instance_id>
      <description>{ description }</description>
      { resultType.toI2b2 }
      <set_size>{ setSize }</set_size>
      <start_date>{ date }</start_date>
      <end_date>{ date }</end_date>
      <query_status_type>
        <name>{ statusType }</name>
        <status_type_id>3</status_type_id><description>FINISHED</description>
      </query_status_type>
      <breakdown_data>
        <resultType>{ PATIENT_AGE_COUNT_XML }</resultType>
        <column>
          <name>bar</name>
          <value>2</value>
        </column>
        <column>
          <name>foo</name>
          <value>1</value>
        </column>
      </breakdown_data>
      <breakdown_data>
        <resultType>{ PATIENT_GENDER_COUNT_XML }</resultType>
        <column>
          <name>huh</name>
          <value>7</value>
        </column>
        <column>
          <name>yeah</name>
          <value>8</value>
        </column>
      </breakdown_data>
      <breakdown_data>
        <resultType>{ PATIENT_RACE_COUNT_XML }</resultType>
        <column>
          <name>nuh</name>
          <value>3</value>
        </column>
        <column>
          <name>zuh</name>
          <value>4</value>
        </column>
      </breakdown_data>
      <breakdown_data>
        <resultType>{ PATIENT_VITALSTATUS_COUNT_XML }</resultType>
        <column>
          <name>blarg</name>
          <value>5</value>
        </column>
        <column>
          <name>glarg</name>
          <value>6</value>
        </column>
      </breakdown_data>
    </query_result_instance>
  }.toString

  private val expectedI2b2ErrorXml = XmlUtil.stripWhitespace {
    <query_result_instance>
      <result_instance_id>0</result_instance_id>
      <query_instance_id>0</query_instance_id>
      <description>{ description }</description>
      <query_result_type>
        <name></name>
      </query_result_type>
      <set_size>0</set_size>
      <query_status_type>
        <name>ERROR</name>
        <description>{ statusMessage }</description>
      </query_status_type>
    </query_result_instance>
  }.toString

  private val expectedI2b2ErrorWithProblemDigestXml = XmlUtil.stripWhitespace {
    <query_result_instance>
      <result_instance_id>0</result_instance_id>
      <query_instance_id>0</query_instance_id>
      <description>{ description }</description>
      <query_result_type>
        <name></name>
      </query_result_type>
      <set_size>0</set_size>
      <query_status_type>
        <name>ERROR</name>
        <description>{ statusMessage }</description>
        <problem>
          <codec>{ problemCodec }</codec>
          <summary>{ problemSummary }</summary>
          <description>{ problemDescription }</description>
          <details>{ problemDetails }</details>
        </problem>
      </query_status_type>
    </query_result_instance>
  }.toString



  //NB: See https://open.med.harvard.edu/jira/browse/SHRINE-745
  private val expectedI2b2IncompleteXml = XmlUtil.stripWhitespace {
    <query_result_instance>
      <result_instance_id>0</result_instance_id>
      <query_instance_id>0</query_instance_id>
      <description>{ description }</description>
      <query_result_type>
        <name></name>
      </query_result_type>
      <set_size>0</set_size>
      <query_status_type>
        <name>INCOMPLETE</name>
        <description>{ statusMessage }</description>
      </query_status_type>
    </query_result_instance>
  }.toString

  import scala.xml.XML.loadString

  //NB: See https://open.med.harvard.edu/jira/browse/SHRINE-745
  @Test
  def testParseIncomplete() {
    val qr = QueryResult.fromI2b2(breakdownTypes.toSet)(loadString(expectedI2b2IncompleteXml))

    qr.statusType should be(QueryResult.StatusType.Incomplete)
  }

  @Test
  def testElapsed() {
    queryResult.copy(startDate = None).elapsed should be(None)
    queryResult.copy(endDate = None).elapsed should be(None)

    queryResult.copy(startDate = None, endDate = None).elapsed should be(None)

    {
      val now = XmlDateHelper.now

      queryResult.copy(startDate = Some(now), endDate = Some(now)).elapsed should equal(Some(0L))
    }

    {
      val start = XmlDateHelper.now

      val delta = 123L

      import XmlGcEnrichments._
      import scala.concurrent.duration._

      val end = start + delta.milliseconds

      queryResult.copy(startDate = Some(start), endDate = Some(end)).elapsed should equal(Some(delta))
    }
  }

  @Test
  def testIsError() {
    queryResult.isError should be(false)

    queryResult.copy(statusType = QueryResult.StatusType.Processing).isError should be(false)
    queryResult.copy(statusType = QueryResult.StatusType.Finished).isError should be(false)
    queryResult.copy(statusType = QueryResult.StatusType.Queued).isError should be(false)
    queryResult.copy(statusType = QueryResult.StatusType.Incomplete).isError should be(false)

    queryResult.copy(statusType = QueryResult.StatusType.Error).isError should be(true)
  }

  @Test
  def testToXml() {
    val queryResultForShrine = queryResult.copy(statusMessage = Some(statusMessage))

    val expectedWhenNoBreakdowns = XmlUtil.stripWhitespace {
      <queryResult>
        <resultId>{ resultId }</resultId>
        <instanceId>{ instanceId }</instanceId>
        { resultType.toXml }
        <setSize>{ setSize }</setSize>
        <startDate>{ date }</startDate>
        <endDate>{ date }</endDate>
        <description>{ description }</description>
        <status>{ statusType }</status>
        <statusMessage>{ statusMessage }</statusMessage>
      </queryResult>
    }.toString

    queryResultForShrine.copy(statusMessage = Some(statusMessage)).toXmlString should equal(expectedWhenNoBreakdowns)

    val expectedWhenNoStartDate = XmlUtil.stripWhitespace {
      <queryResult>
        <resultId>{ resultId }</resultId>
        <instanceId>{ instanceId }</instanceId>
        { resultType.toXml }
        <setSize>{ setSize }</setSize>
        <endDate>{ date }</endDate>
        <description>{ description }</description>
        <status>{ statusType }</status>
        <statusMessage>{ statusMessage }</statusMessage>
      </queryResult>
    }.toString

    queryResultForShrine.copy(startDate = None).toXmlString should equal(expectedWhenNoStartDate)

    val expectedWhenNoEndDate = XmlUtil.stripWhitespace {
      <queryResult>
        <resultId>{ resultId }</resultId>
        <instanceId>{ instanceId }</instanceId>
        { resultType.toXml }
        <setSize>{ setSize }</setSize>
        <startDate>{ date }</startDate>
        <description>{ description }</description>
        <status>{ statusType }</status>
        <statusMessage>{ statusMessage }</statusMessage>
      </queryResult>
    }.toString

    queryResultForShrine.copy(endDate = None).toXmlString should equal(expectedWhenNoEndDate)

    val expectedWhenNoDescription = XmlUtil.stripWhitespace {
      <queryResult>
        <resultId>{ resultId }</resultId>
        <instanceId>{ instanceId }</instanceId>
        { resultType.toXml }
        <setSize>{ setSize }</setSize>
        <startDate>{ date }</startDate>
        <endDate>{ date }</endDate>
        <status>{ statusType }</status>
        <statusMessage>{ statusMessage }</statusMessage>
      </queryResult>
    }.toString

    queryResultForShrine.copy(description = None).toXmlString should equal(expectedWhenNoDescription)

    val expectedWhenNoStatusMessage = XmlUtil.stripWhitespace {
      <queryResult>
        <resultId>{ resultId }</resultId>
        <instanceId>{ instanceId }</instanceId>
        { resultType.toXml }
        <setSize>{ setSize }</setSize>
        <startDate>{ date }</startDate>
        <endDate>{ date }</endDate>
        <description>{ description }</description>
        <status>{ statusType }</status>
      </queryResult>
    }.toString

    queryResult.copy(statusMessage = None).toXmlString should equal(expectedWhenNoStatusMessage)

    resultWithBreakDowns.toXmlString should equal(expectedWhenBreakdownsArePresent)
  }

  @Test
  def testFromXml() {
    QueryResult.fromXml(breakdownTypes.toSet)(loadString(expectedWhenBreakdownsArePresent)) should equal(resultWithBreakDowns)
  }

  @Test
  def testShrineRoundTrip() = {
    QueryResult.fromXml(breakdownTypes.toSet)(resultWithBreakDowns.toXml) should equal(resultWithBreakDowns)
  }

  private def compareIgnoringBreakdowns(actual: QueryResult, expected: QueryResult) {
    //Ignore breakdowns field, since this can't be serialized to i2b2 format as part of a <query_result_instance>
    actual.breakdowns should equal(Map.empty)
    actual.description should equal(expected.description)
    actual.endDate should equal(expected.endDate)
    actual.instanceId should equal(expected.instanceId)
    actual.resultId should equal(expected.resultId)
    actual.resultType should equal(expected.resultType)
    actual.setSize should equal(expected.setSize)
    actual.startDate should equal(expected.startDate)
    actual.statusMessage should equal(expected.statusMessage)
    actual.statusType should equal(expected.statusType)
  }

  @Test
  def testI2b2RoundTrip() = {
    //NB: Needed because i2b2 handles status messages differently. In the error case, statusMessage is
    //descriptive; otherwise, it's the all-caps name of the status type.  This is different from how
    //Shrine creates and parses statusMessage XML, so we need a new QueryResult here.  (Previously, we
    //could use the same one, since we were ignoring statusMessage and description when unmarshalling
    //from i2b2 format.) 

    val newStatusMessage = Some(resultWithBreakDowns.statusType.name)

    val resultWithBreakDownsForI2b2 = resultWithBreakDowns.copy(statusMessage = newStatusMessage)

    val unmarshalled = QueryResult.fromI2b2(breakdownTypes.toSet)(resultWithBreakDownsForI2b2.toI2b2)

    compareIgnoringBreakdowns(unmarshalled, resultWithBreakDownsForI2b2)
  }

  @Test
  def testFromI2b2() {
    compareIgnoringBreakdowns(QueryResult.fromI2b2(breakdownTypes.toSet)(loadString(expectedI2b2Xml)), queryResult)
  }

  @Test
  def testFromI2b2WithErrors() {
    val errorResult = QueryResult.errorResult(Some(description), statusMessage)

    val actual = QueryResult.fromI2b2(breakdownTypes.toSet)(loadString(expectedI2b2ErrorXml))

    compareIgnoringBreakdowns(actual, errorResult)
  }

  @Test
  def testToI2b2() {
    queryResult.toI2b2String should equal(expectedI2b2Xml)
  }

  @Test
  def testToI2b2WithBreakdowns() {
    resultWithBreakDowns.toI2b2String should equal(expectedI2b2XmlWithBreakdowns)
  }

  @Test
  def testToI2b2AllStatusTypes(): Unit = {
    def doTest(statusType: QueryResult.StatusType) {
      val expectedI2b2Xml = XmlUtil.stripWhitespace {
        <query_result_instance>
          <result_instance_id>{ resultId }</result_instance_id>
          <query_instance_id>{ instanceId }</query_instance_id>
          <description>{ description }</description>
          { resultType.toI2b2 }
          <set_size>{ setSize }</set_size>
          <start_date>{ date }</start_date>
          <end_date>{ date }</end_date>
          <query_status_type>
            <name>{ statusType }</name>
            <status_type_id>{ statusType.i2b2Id.get }</status_type_id><description>{ statusType }</description>
          </query_status_type>
        </query_result_instance>
      }.toString

      val result = queryResult.copy(statusType = statusType)

      result.toI2b2String should equal(expectedI2b2Xml)
    }

    import QueryResult.StatusType

    //NB: Error is tested by testToI2b2WithErrors()
    val nonErrorStatuses = StatusType.values.toSet - StatusType.Error

    for (statusType <- nonErrorStatuses) {
      doTest(statusType)
    }
  }

  @Test
  def testToI2b2WithErrors(): Unit = {
    val actual = QueryResult.errorResult(Some(description), statusMessage).toI2b2String

    actual should equal(expectedI2b2ErrorXml)
  }

  @Test
  def testWithErrorsAndProblemDigest():Unit = {

    val actual = QueryResult.errorResult(
      Some(description),
      statusMessage,
      Option(ProblemDigest(problemCodec,problemSummary,problemDescription,problemDetails)))

    val i2b2String = actual.toI2b2String

    i2b2String should equal(expectedI2b2ErrorWithProblemDigestXml)

    val i2b2 = actual.toI2b2
    val fromI2b2 = QueryResult.fromI2b2(Set.empty)(i2b2)

    println(i2b2)

    println(fromI2b2)

    fromI2b2 should equal(actual)

    val xml = actual.toXml
    val fromXml = QueryResult.fromXml(Set.empty)(xml)
    fromXml should equal(actual)
  }
}