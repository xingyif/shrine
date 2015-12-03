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
final class ReadApprovedQueryTopicsRequestTest extends ShrineRequestValidator {

  override def messageBody = XmlUtil.stripWhitespace {
    <message_body>
      <ns8:sheriff_header xsi:type="ns8:sheriffHeaderType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
      <ns8:sheriff_request xsi:type="ns8:sheriffRequestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
    </message_body>
  }

  val readApprovedQueryTopicsRequest = XmlUtil.stripWhitespace {
    <readApprovedQueryTopics>
      { requestHeaderFragment }
      <userId>{ username }</userId>
    </readApprovedQueryTopics>
  }

  @Test
  override def testFromI2b2 {
    val translatedRequest = ReadApprovedQueryTopicsRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get
    
    validateRequestWith(translatedRequest) {
      translatedRequest.userId should equal(username)
    }
  }

  @Test
  override def testShrineRequestFromI2b2 {
    val shrineRequest = HandleableShrineRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get

    shrineRequest.isInstanceOf[ReadApprovedQueryTopicsRequest] should be(true)
  }

  @Test
  override def testToI2b2 {
    ReadApprovedQueryTopicsRequest(projectId, waitTime, authn, username).toI2b2 should equal(request)
  }

  @Test
  override def testToXml {
    ReadApprovedQueryTopicsRequest(projectId, waitTime, authn, username).toXml should equal(readApprovedQueryTopicsRequest)
  }

  @Test
  override def testFromXml {
    val request = ReadApprovedQueryTopicsRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(readApprovedQueryTopicsRequest).get
    
    validateRequestWith(request) {
      request.userId should equal(username)
    }
  }

  @Test
  def testShrineRequestFromXml {
    ShrineRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(readApprovedQueryTopicsRequest).get.isInstanceOf[ReadApprovedQueryTopicsRequest] should be(true)
  }
}