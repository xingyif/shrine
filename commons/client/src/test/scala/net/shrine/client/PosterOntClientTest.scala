package net.shrine.client

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.{HiveCredentials, AuthenticationInfo, Credential, RequestType}
import net.shrine.util.XmlUtil

/**
 * @author clint
 * @date Jan 27, 2014
 */
final class PosterOntClientTest extends ShouldMatchersForJUnit {

  import scala.concurrent.duration._

  private val projectId = "SHRINE"
  private val waitTime = 1.minute
  private val authn = AuthenticationInfo("d", "u", Credential("p", false))
  private val shrineVersionTerm = """\\SHRINE\SHRINE\ONTOLOGYVERSION\1.7_release_09.30.2010\"""

  private lazy val hiveCredentials = HiveCredentials(authn.domain, authn.username, authn.credential.value, projectId)
    
  @Test
  def testChildrenOf {
    final class MyMockHttpClient(toReturn: String) extends MockHttpClient {
      override def post(input: String, url: String): HttpResponse = {
        lastInput = Some(input)
        lastUrl = Some(url)
      
        HttpResponse.ok(toReturn)
      }
    }
    
    val httpClient = new MyMockHttpClient(responseXml.toString)
    
    val ontUrl = "http://example.com:9090/i2b2/rest/OntologyService/"
    
    val poster = Poster(ontUrl, httpClient)
    
    import scala.concurrent.duration._
    
    val waitTime = 1.minute
    
    val posterOntClient = new PosterOntClient(hiveCredentials, waitTime, poster)
    
    val parentTerm = """\\SHRINE\SHRINE\ONTOLOGYVERSION\"""
      
    val children = posterOntClient.childrenOf(parentTerm)
    
    children should equal(Set(shrineVersionTerm))
    
    httpClient.lastUrl.get should equal(ontUrl + "getChildren")
    httpClient.lastInput.get should equal(PosterOntClient.ReadOntChildNodesRequest(projectId, waitTime, hiveCredentials.toAuthenticationInfo, parentTerm).toI2b2String)
  }
  
  @Test
  def testReadOntChildNodesRequest {
    import PosterOntClient.ReadOntChildNodesRequest

    val parentTerm = """\\FOO\BAR\BAZ"""

    val req = ReadOntChildNodesRequest(projectId, waitTime, authn, parentTerm)

    req.requestType should be(RequestType.ReadOntChildTerms)

    intercept[NotImplementedError] {
      req.toXml
    }

    intercept[NotImplementedError] {
      req.toXmlString
    }

    val expectedI2b2Xml = XmlUtil.stripWhitespace {
      <message_body>
        <ns100:get_children blob="true" type="default" max='100' synonyms="false" hiddens="true">
          <parent>{ parentTerm }</parent>
        </ns100:get_children>
      </message_body>
    }.toString

    req.i2b2MessageBody.toString should equal(expectedI2b2Xml)
  }

  @Test
  def testExtractChildTermsFromI2b2Response {
    PosterOntClient.extractChildTermsFromI2b2Response(None) should equal(Set.empty)
    
    val childTerms = PosterOntClient.extractChildTermsFromI2b2Response(Some(responseXml))
    
    childTerms should equal(Set(shrineVersionTerm))
    
    PosterOntClient.extractChildTermsFromI2b2Response(Some(<foo/>)) should equal(Set.empty)
  }
  
  private lazy val responseXml = {
      <ns4:response xmlns:ns6="http://www.i2b2.org/xsd/cell/pm/1.1/" xmlns:tns="http://ws.ontology.i2b2.harvard.edu" xmlns:ns5="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/loader/1.1/" xmlns:ns4="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns2="http://www.i2b2.org/xsd/cell/fr/1.0/">
        <message_header>
          <i2b2_version_compatible>1.1</i2b2_version_compatible>
          <hl7_version_compatible>2.4</hl7_version_compatible>
          <sending_application>
            <application_name>Ontology Cell</application_name>
            <application_version>1.5</application_version>
          </sending_application>
          <sending_facility>
            <facility_name>i2b2 Hive</facility_name>
          </sending_facility>
          <receiving_application>
            <application_name>i2b2_QueryTool</application_name>
            <application_version>0.2</application_version>
          </receiving_application>
          <receiving_facility>
            <facility_name>SHRINE</facility_name>
          </receiving_facility>
          <datetime_of_message>2014-01-27T16:22:58.488-05:00</datetime_of_message>
          <security>
            <domain>i2b2demo</domain>
            <username>demo</username>
            <password is_token="false" token_ms_timeout="1800000">demouser</password>
          </security>
          <message_control_id>
            <message_num>EQ7Szep1Md11K4E7zEc99</message_num>
            <instance_num>1</instance_num>
          </message_control_id>
          <processing_id>
            <processing_id>P</processing_id>
            <processing_mode>I</processing_mode>
          </processing_id>
          <accept_acknowledgement_type>AL</accept_acknowledgement_type>
          <application_acknowledgement_type>AL</application_acknowledgement_type>
          <country_code>US</country_code>
          <project_id>SHRINE</project_id>
        </message_header>
        <response_header>
          <result_status>
            <status type="DONE">Ontology processing completed</status>
          </result_status>
        </response_header>
        <message_body>
          <ns5:concepts>
            <concept>
              <level>2</level>
              <key>{ shrineVersionTerm }</key>
              <name>ONTOLOGYVERSION</name>
              <synonym_cd>N</synonym_cd>
              <visualattributes>LH </visualattributes>
              <totalnum>0</totalnum>
              <facttablecolumn>concept_cd</facttablecolumn>
              <tablename>concept_dimension</tablename>
              <columnname>concept_path</columnname>
              <columndatatype>T</columndatatype>
              <operator>LIKE</operator>
              <dimcode>\SHRINE\ONTOLOGYVERSION\</dimcode>
              <tooltip>02-NOV-10</tooltip>
            </concept>
          </ns5:concepts>
        </message_body>
      </ns4:response>
    }
}