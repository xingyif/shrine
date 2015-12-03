package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.XmlUtil

/**
 * @author clint
 * @date Oct 6, 2014
 */
final class ReadResultOutputTypesResponseTest extends ShouldMatchersForJUnit {
  @Test
  def testToI2b2: Unit = {
    val expectedMessageBody = XmlUtil.stripWhitespace {
      <ns4:response xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns4:result_type_responseType">
        <status>
          <condition type="DONE">DONE</condition>
        </status>
        <query_result_type>
          <result_type_id>1</result_type_id>
          <name>PATIENTSET</name>
          <display_type>LIST</display_type>
          <visual_attribute_type>LA</visual_attribute_type>
          <description>Patient set</description>
        </query_result_type>
        <query_result_type>
          <result_type_id>2</result_type_id>
          <name>PATIENT_COUNT_XML</name>
          <display_type>CATNUM</display_type>
          <visual_attribute_type>LA</visual_attribute_type>
          <description>Number of patients</description>
        </query_result_type>
        <query_result_type>
          <result_type_id>3</result_type_id>
          <name>PATIENT_GENDER_COUNT_XML</name>
          <display_type>CATNUM</display_type>
          <visual_attribute_type>LA</visual_attribute_type>
          <description>Gender patient breakdown</description>
        </query_result_type>
        <query_result_type>
          <result_type_id>4</result_type_id>
          <name>PATIENT_VITALSTATUS_COUNT_XML</name>
          <display_type>CATNUM</display_type>
          <visual_attribute_type>LA</visual_attribute_type>
          <description>Vital Status patient breakdown</description>
        </query_result_type>
        <query_result_type>
          <result_type_id>5</result_type_id>
          <name>PATIENT_RACE_COUNT_XML</name>
          <display_type>CATNUM</display_type>
          <visual_attribute_type>LA</visual_attribute_type>
          <description>Race patient breakdown</description>
        </query_result_type>
        <query_result_type>
          <result_type_id>6</result_type_id>
          <name>PATIENT_AGE_COUNT_XML</name>
          <display_type>CATNUM</display_type>
          <visual_attribute_type>LA</visual_attribute_type>
          <description>Age patient breakdown</description>
        </query_result_type>
      </ns4:response>
    }

    import ResultOutputType._
    import DefaultBreakdownResultOutputTypes._

    val resp = ReadResultOutputTypesResponse(Seq(PATIENTSET, PATIENT_COUNT_XML, PATIENT_GENDER_COUNT_XML, PATIENT_VITALSTATUS_COUNT_XML, PATIENT_RACE_COUNT_XML, PATIENT_AGE_COUNT_XML))

    val messageBody = resp.i2b2MessageBody

    messageBody.toString should equal(expectedMessageBody.toString)
  }
}