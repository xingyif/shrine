package net.shrine.proxy

import scala.xml.NodeSeq
import net.shrine.client.HttpResponse
import net.shrine.util.XmlUtil
import net.shrine.client.HttpClient
import net.shrine.util.ShouldMatchersForJUnit

/**
 * [ Author ]
 *
 * @author Clint Gilbert
 * @author Ricardo Delima
 * @author Andrew McMurry
 *
 * Date: Apr 1, 2008
 * Harvard Medical School Center for BioMedical Informatics
 *
 * @link http://cbmi.med.harvard.edu
 */
final class DefaultShrineProxyTest extends ShouldMatchersForJUnit {

  import DefaultShrineProxyTest._

  def testRedirect {
    val whiteList = Set("http://example.com")

    val blackList = Set("http://malware.com")

    val shouldWork = Seq("http://example.com", "http://example.com/foo", "http://example.com/foo/lots/of/stuff?blah=nuh")

    val statuses = Seq(200, 400, 500)
    
    for {
      url <- shouldWork
      statusCode <- statuses
    } {
      val mockUrlPoster = new DefaultShrineProxyTest.MockHttpClient(statusCode)

      val proxy = new DefaultShrineProxy(whiteList, blackList, mockUrlPoster)

      val inputXml = getQuery(url)

      proxy.redirect(inputXml) should equal(HttpResponse(statusCode, "OK"))

      mockUrlPoster.input should equal(inputXml.toString)
      mockUrlPoster.url should equal(url)
    }
    
    val shouldFail = Seq("http://google.com", "https://example.com", null, "", "  ")

    for {
      url <- shouldFail
      statusCode <- statuses
    } {
      val mockUrlPoster = new MockHttpClient(statusCode)

      val proxy = new DefaultShrineProxy(whiteList, blackList, mockUrlPoster)

      intercept[ShrineMessageFormatException] {
        proxy.redirect(getQuery(url))
      }
    }
  }
}

object DefaultShrineProxyTest {
  final class MockHttpClient(statusCode: Int) extends HttpClient {
    var url: String = _
    var input: String = _

    override def post(input: String, url: String): HttpResponse = {
      this.url = url
      this.input = input

      HttpResponse(statusCode, "OK")
    }
  }

  def getQuery(url: String): NodeSeq = {
    XmlUtil.stripWhitespace {
      <ns6:request xmlns:ns4="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns7="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/hive/plugin/" xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns8="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/">
        <message_header>
          <proxy><redirect_url>{ url }</redirect_url></proxy>
          <sending_application>
            <application_name>i2b2_QueryTool</application_name>
            <application_version>0.2</application_version>
          </sending_application>
          <sending_facility>
            <facility_name>PHS</facility_name>
          </sending_facility>
          <receiving_application>
            <application_name>i2b2_DataRepositoryCell</application_name>
            <application_version>0.2</application_version>
          </receiving_application>
          <receiving_facility>
            <facility_name>PHS</facility_name>
          </receiving_facility>
          <security>
            <domain>Harvard Demo</domain>
            <username>demo</username>
            <password>demouser</password>
          </security>
          <message_type>
            <message_code>Q04</message_code>
            <event_type>EQQ</event_type>
          </message_type>
          <message_control_id>
            <message_num>fXO4nxn7O2i9hGPrEgqW</message_num>
            <instance_num>0</instance_num>
          </message_control_id>
          <processing_id>
            <processing_id>P</processing_id>
            <processing_mode>I</processing_mode>
          </processing_id>
          <accept_acknowledgement_type>messageId</accept_acknowledgement_type>
          <project_id xsi:nil="true\ xmlns:xsi=\http://www.w3.org/2001/XMLSchema-instance"/>
        </message_header>
        <request_header>
          <result_waittime_ms>180000</result_waittime_ms>
        </request_header>
        <message_body>
          <ns4:psmheader>
            <user>demo</user>
            <patient_set_limit>0</patient_set_limit>
            <estimated_time>0</estimated_time>
            <request_type>CRC_QRY_runQueryInstance_fromQueryDefinition</request_type>
          </ns4:psmheader>
          <ns4:request xsi:type="ns4:query_definition_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <query_definition>
              <query_name>0-9 years old@05:42:40</query_name>
              <specificity_scale>0</specificity_scale>
              <panel>
                <panel_number>1</panel_number>
                <invert>0</invert>
                <total_item_occurrences>1</total_item_occurrences>
                <item>
                  <hlevel>3</hlevel>
                  <item_name>0-9 years old</item_name>
                  <item_key>\\\\i2b2\\i2b2\\Demographics\\Age\\0-9 years old</item_key>
                  <tooltip>Demographic \\ Age \\ 0-9 years old</tooltip>
                  <class>ENC</class>
                  <constrain_by_date/>
                </item>
              </panel>
            </query_definition>
          </ns4:request>
        </message_body>
      </ns6:request>
    }
  }
}