package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date May 2, 2014
 */
final class RunHeldQueryRequestTest extends ShouldMatchersForJUnit {
  import scala.concurrent.duration._

  private val req = RunHeldQueryRequest("projectId", 1.hour, AuthenticationInfo("d", "u", Credential("p", false)), 12345L)

  @Test
  def testShrineXmlRoundTrip {
    RunHeldQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(req.toXml).get should equal(req)
  }

  @Test
  def testI2b2XmlRoundTrip {
    RunHeldQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(req.toI2b2).get should equal(req)
  }

  @Test
  def testFromI2b2 {
    val xml = {
      <ns6:request xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns8="http://sheriff.shrine.net/" xmlns:ns7="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/hive/plugin/" xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns99="http://www.i2b2.org/xsd/cell/pm/1.1/" xmlns:ns100="http://www.i2b2.org/xsd/cell/ont/1.1/"><message_header><proxy><redirect_url>https://localhost/shrine/QueryToolService/request</redirect_url></proxy><sending_application><application_name>i2b2_QueryTool</application_name><application_version>0.2</application_version></sending_application><sending_facility><facility_name>SHRINE</facility_name></sending_facility><receiving_application><application_name>i2b2_DataRepositoryCell</application_name><application_version>0.2</application_version></receiving_application><receiving_facility><facility_name>SHRINE</facility_name></receiving_facility><security><domain>Some-other-domain</domain><username>some-other-user</username><password is_token="false" token_ms_timeout="1800000">some-val</password></security><message_type><message_code>Q04</message_code><event_type>EQQ</event_type></message_type><message_control_id><message_num>EQ7Szep1Md11K4E7zEc99</message_num><instance_num>0</instance_num></message_control_id><processing_id><processing_id>P</processing_id><processing_mode>I</processing_mode></processing_id><accept_acknowledgement_type>AL</accept_acknowledgement_type><project_id>some-project-id</project_id><country_code>US</country_code></message_header><request_header><result_waittime_ms>12345</result_waittime_ms></request_header><message_body><runHeldQuery><networkQueryId>12345</networkQueryId></runHeldQuery></message_body></ns6:request>
    }

    val req = RunHeldQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(xml).get
    
    req.networkQueryId should be(12345L)
  }
}