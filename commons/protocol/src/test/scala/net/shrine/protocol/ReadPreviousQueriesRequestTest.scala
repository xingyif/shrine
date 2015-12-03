package net.shrine.protocol

import org.junit.Test

import net.shrine.util.XmlUtil

/**
 * @author Bill Simons
 * @date 3/11/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class ReadPreviousQueriesRequestTest extends ShrineRequestValidator {
  val fetchSize = 20

  override def messageBody = XmlUtil.stripWhitespace {
    <message_body>
      <ns4:psmheader>
        <user login={ username }>{ username }</user>
        <patient_set_limit>0</patient_set_limit>
        <estimated_time>0</estimated_time>
        <request_type>CRC_QRY_getQueryMasterList_fromUserId</request_type>
      </ns4:psmheader>
      <ns4:request xsi:type="ns4:user_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <user_id>{ username }</user_id>
        <group_id>{ projectId }</group_id>
        <fetch_size>{ fetchSize }</fetch_size>
      </ns4:request>
    </message_body>
  }

  val readPreviousQueriesRequest = XmlUtil.stripWhitespace {
    <readPreviousQueries>
      { requestHeaderFragment }
      <userId>{ username }</userId>
      <fetchSize>{ fetchSize }</fetchSize>
    </readPreviousQueries>
  }

  @Test
  override def testFromI2b2 {
    val translatedRequest = ReadPreviousQueriesRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get

    validateRequestWith(translatedRequest) {
      translatedRequest.userId should equal(username)
      translatedRequest.fetchSize should equal(fetchSize)
    }
  }

  @Test
  override def testShrineRequestFromI2b2 {
    val shrineRequest = CrcRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get

    shrineRequest.isInstanceOf[ReadPreviousQueriesRequest] should be(true)
  }

  @Test
  def testDoubleDispatchingShrineRequestFromI2b2 {
    val shrineRequest = HandleableShrineRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get

    shrineRequest.isInstanceOf[ReadPreviousQueriesRequest] should be(true)
  }

  @Test
  override def testToXml {
    ReadPreviousQueriesRequest(projectId, waitTime, authn, username, fetchSize).toXml should equal(readPreviousQueriesRequest)
  }

  @Test
  override def testToI2b2 {
    ReadPreviousQueriesRequest(projectId, waitTime, authn, username, fetchSize).toI2b2 should equal(request)
  }

  @Test
  override def testFromXml {
    val actual = ReadPreviousQueriesRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(readPreviousQueriesRequest).get

    validateRequestWith(actual) {
      actual.userId should equal(username)
      actual.fetchSize should equal(fetchSize)
    }
  }

  @Test
  def testShrineRequestFromXml {
    ShrineRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(readPreviousQueriesRequest).get.isInstanceOf[ReadPreviousQueriesRequest] should be(true)
  }
}