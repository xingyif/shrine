package net.shrine.i2b2.protocol.pm

import org.junit.Test
import net.shrine.util.XmlUtil
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author Bill Simons
 * @date 3/5/12
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class HiveConfigTest extends ShouldMatchersForJUnit {

  val crcUrl = "https://localhost:8443/shrine/rest/i2b2/"
  val ontologyUrl = "http://localhost:9090/i2b2/rest/OntologyService/"

  def response = XmlUtil.stripWhitespace {
      <ns4:response xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns4="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/cell/pm/1.1/" xmlns:ns7="http://sheriff.shrine.net/" xmlns:ns8="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns9="http://www.i2b2.org/xsd/cell/crc/psm/analysisdefinition/1.1/" xmlns:ns10="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns11="http://www.i2b2.org/xsd/hive/msg/result/1.1/">
        <message_header>
          <i2b2_version_compatible>1.1</i2b2_version_compatible>
          <hl7_version_compatible>2.4</hl7_version_compatible>
          <sending_application>
            <application_name>SHRINE</application_name>
            <application_version>1.3-compatible</application_version>
          </sending_application>
          <sending_facility>
            <facility_name>SHRINE</facility_name>
          </sending_facility>
          <datetime_of_message>2011-04-08T16:21:12.251-04:00</datetime_of_message>
          <security/>
          <project_id xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
        </message_header>
        <response_header>
          <result_status>
            <status type="DONE">DONE</status>
          </result_status>
        </response_header>
        <message_body>
                <ns6:configure>
                    <environment>DEVELOPMENT</environment>
                    <helpURL>http://www.i2b2.org</helpURL>
                    <user>
                        <full_name>Full name</full_name>
                        <user_name>user name</user_name>
                        <password>password</password>
                        <domain>demo</domain>
                        <project id="Demo">
                            <name>Demo Group</name>
                            <wiki>http://www.i2b2.org</wiki>
                            <role>DATA_OBFSC</role>
                        </project>
                    </user>
                    <cell_datas>
                        <cell_data id="CRC">
                            <name>Data Repository</name>
                            <url>{crcUrl}</url>
                            <method>REST</method>
                        </cell_data>
                        <cell_data id="ONT">
                            <name>Ontology Cell</name>
                            <url>{ontologyUrl}</url>
                            <method>REST</method>
                            <param name="OntSynonyms">false</param>
                            <param name="OntMax">200</param>
                            <param name="OntHidden">false</param>
                        </cell_data>
                    </cell_datas>
                </ns6:configure>
            </message_body>
      </ns4:response>
  }

  @Test
  def fromI2b2() {
    val hiveConfig = HiveConfig.fromI2b2(response)
    hiveConfig.crcUrl should equal(crcUrl)
    hiveConfig.ontologyUrl should equal(ontologyUrl)
  }
}