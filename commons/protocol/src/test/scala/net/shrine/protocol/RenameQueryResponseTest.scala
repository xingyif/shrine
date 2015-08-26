package net.shrine.protocol

import xml.Utility
import org.junit.Test
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
class RenameQueryResponseTest extends ShrineResponseI2b2SerializableValidator {
  val queryId = 123456789L
  val queryName = "name"

  def messageBody = <message_body>
      <ns6:response xsi:type="ns6:master_responseType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <status>
        <condition type="DONE">DONE</condition>
      </status>
      <query_master>
        <query_master_id>{queryId}</query_master_id>
        <name>{queryName}</name>
      </query_master>
    </ns6:response>
  </message_body>

  val renameQueryResponse = XmlUtil.stripWhitespace(
    <renameQueryResponse>
      <queryId>{queryId}</queryId>
      <queryName>{queryName}</queryName>
    </renameQueryResponse>)

  @Test
  def testFromXml() {
    val actual = RenameQueryResponse.fromXml(renameQueryResponse)
    actual.queryId should equal(queryId)
    actual.queryName should equal(queryName)
  }

  @Test
  def testToXml() {
    new RenameQueryResponse(queryId, queryName).toXml should equal(renameQueryResponse)
  }

  @Test
  def testFromI2b2() {
    val translatedResponse = RenameQueryResponse.fromI2b2(response)
    translatedResponse.queryId should equal(queryId)
    translatedResponse.queryName should equal(queryName)
  }

  @Test
  def testToI2b2() {
    new RenameQueryResponse(queryId, queryName).toI2b2 should equal(response)
  }
}