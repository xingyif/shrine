package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.util.XmlUtil

/**
 * @author clint
 * @date Oct 3, 2014
 */
final class ReadResultOutputTypesRequestTest extends ShouldMatchersForJUnit {
  import ReadResultOutputTypesRequestTest._

  @Test
  def testToXml: Unit = {
    val xml = req.toXml

    xml should equal(XmlUtil.stripWhitespace {
      <readResultOutputTypesRequest>
        <projectId>{ projectId }</projectId>
        <waitTimeMs>{ waitTime.toMillis }</waitTimeMs>
        { authn.toXml }
      </readResultOutputTypesRequest>
    })
  }

  @Test
  def testFromI2b2: Unit = {

    import ReadResultOutputTypesRequest.fromI2b2
    
    fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(null).isFailure should be(true)
    fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(<foo/>).isFailure should be(true)
    
    val nonReadResultOutputTypeCrcReqTypes = CrcRequestType.values.filterNot(_ == CrcRequestType.GetResultOutputTypes)
    
    for {
      otherReqType <- nonReadResultOutputTypeCrcReqTypes
    } {
      fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(i2b2Xml(otherReqType)).isFailure should be(true)
    }
    
    val unmarshalled = fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(i2b2Xml(CrcRequestType.GetResultOutputTypes)).get
    
    unmarshalled.authn should equal(authn)
    unmarshalled.projectId should equal(projectId)
    unmarshalled.waitTime should equal(waitTime)
  }
  
  @Test
  def testIsReadResultOutputTypesTest: Unit = {
    import ReadResultOutputTypesRequest.isReadResultOutputTypesRequest
    
    val nonReadResultOutputTypeCrcReqTypes = CrcRequestType.values.filterNot(_ == CrcRequestType.GetResultOutputTypes)
    
    for {
      otherReqType <- nonReadResultOutputTypeCrcReqTypes
    } {
      isReadResultOutputTypesRequest(i2b2Xml(otherReqType)) should be(false)
    }
    
    isReadResultOutputTypesRequest(i2b2Xml(CrcRequestType.GetResultOutputTypes)) should be(true)
  }
}

object ReadResultOutputTypesRequestTest {
  import scala.concurrent.duration._
  
  val projectId = "pid"
  val waitTime = 5.seconds
  val authn = AuthenticationInfo("d", "u", Credential("p", false))
  val req = ReadResultOutputTypesRequest(projectId, waitTime, authn)
  
  def i2b2Xml(crcReqType: CrcRequestType) = {
    <ns6:request xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns7="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/hive/plugin/" xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/hive/msg/1.1/">
      <message_header>
        <proxy>
          <redirect_url>http://services.i2b2.org:9090/i2b2/services/QueryToolService/request</redirect_url>
        </proxy>
        <sending_application>
          <application_name>i2b2_QueryTool</application_name>
          <application_version>1.6</application_version>
        </sending_application>
        <sending_facility>
          <facility_name>PHS</facility_name>
        </sending_facility>
        <receiving_application>
          <application_name>i2b2_DataRepositoryCell</application_name>
          <application_version>1.6</application_version>
        </receiving_application>
        <receiving_facility>
          <facility_name>PHS</facility_name>
        </receiving_facility>
        <message_type>
          <message_code>Q04</message_code>
          <event_type>EQQ</event_type>
        </message_type>
        { authn.toI2b2 }
        <message_control_id>
          <message_num>46r43iy725Q2t4Lw14Or3</message_num>
          <instance_num>0</instance_num>
        </message_control_id>
        <processing_id>
          <processing_id>P</processing_id>
          <processing_mode>I</processing_mode>
        </processing_id>
        <accept_acknowledgement_type>messageId</accept_acknowledgement_type>
        <project_id>{ projectId }</project_id>
      </message_header>
      <request_header>
        <result_waittime_ms>{ waitTime.toMillis }</result_waittime_ms>
      </request_header>
      <message_body>
        <ns4:psmheader>
          <user login="demo">demo</user>
          <patient_set_limit>0</patient_set_limit>
          <estimated_time>0</estimated_time>
          <request_type>{ crcReqType.i2b2RequestType }</request_type>
        </ns4:psmheader>
      </message_body>
    </ns6:request>
  }
}