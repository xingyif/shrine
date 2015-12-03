package net.shrine.protocol

import org.junit.Test
import xml.Utility
import net.shrine.util.XmlUtil

/**
 * @author Bill Simons
 * @date 4/12/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
class DeleteQueryResponseTest extends ShrineResponseI2b2SerializableValidator {
  val queryId = 123456789L

  def messageBody = <message_body>
      <ns6:response xsi:type="ns6:master_responseType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <status>
        <condition type="DONE">DONE</condition>
      </status>
      <query_master>
        <query_master_id>{queryId}</query_master_id>
      </query_master>
    </ns6:response>
  </message_body>

  val deleteQueryResponse = XmlUtil.stripWhitespace(
    <deleteQueryResponse>
      <queryId>{queryId}</queryId>
    </deleteQueryResponse>)

  @Test
  def testFromXml() {
    val actual = DeleteQueryResponse.fromXml(deleteQueryResponse)
    actual.queryId should equal(queryId)
  }

  @Test
  def testToXml() {
    new DeleteQueryResponse(queryId).toXml should equal(deleteQueryResponse)
  }

  @Test
  def testFromI2b2() {
    val translatedResponse = DeleteQueryResponse.fromI2b2(response)
    translatedResponse.queryId should equal(queryId)
  }

  @Test
  def testToI2b2() {
    new DeleteQueryResponse(queryId).toI2b2 should equal(response)
  }
}