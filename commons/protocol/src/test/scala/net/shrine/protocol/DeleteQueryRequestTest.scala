package net.shrine.protocol

import org.junit.Test
import org.junit.Assert.assertTrue
import xml.Utility
import net.shrine.util.XmlUtil

/**
 * @author Bill Simons
 * @date 3/28/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class DeleteQueryRequestTest extends ShrineRequestValidator {
  val queryId = 2422297885846950097L

  override def messageBody = XmlUtil.stripWhitespace {
    <message_body>
      <ns4:psmheader>
        <user login={ authn.username }>{ authn.username }</user>
        <patient_set_limit>0</patient_set_limit>
        <estimated_time>0</estimated_time>
        <request_type>CRC_QRY_deleteQueryMaster</request_type>
      </ns4:psmheader>
      <ns4:request xsi:type="ns4:master_delete_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <user_id>{ authn.username }</user_id>
        <query_master_id>{ queryId }</query_master_id>
      </ns4:request>
    </message_body>
  }

  val deleteQueryRequest = XmlUtil.stripWhitespace {
    <deleteQuery>
      { requestHeaderFragment }
      <queryId>{ queryId }</queryId>
    </deleteQuery>
  }

  @Test
  override def testFromI2b2 {
    val translatedRequest = DeleteQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get
    
    validateRequestWith(translatedRequest) {
      translatedRequest.networkQueryId should equal(queryId)
    }
  }

  @Test
  override def testShrineRequestFromI2b2 {
    assertTrue(CrcRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get.isInstanceOf[DeleteQueryRequest])
  }

  @Test
  def testDoubleDispatchingShrineRequestFromI2b2 {
    assertTrue(HandleableShrineRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get.isInstanceOf[DeleteQueryRequest])
  }

  @Test
  override def testToXml {
    DeleteQueryRequest(projectId, waitTime, authn, queryId).toXml should equal(deleteQueryRequest)
  }

  @Test
  def testToI2b2 {
    DeleteQueryRequest(projectId, waitTime, authn, queryId).toI2b2 should equal(request)
  }

  @Test
  def testFromXml {
    val actual = DeleteQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(deleteQueryRequest).get
    
    validateRequestWith(actual) {
      actual.networkQueryId should equal(queryId)
    }
  }

  @Test
  def testShrineRequestFromXml {
    ShrineRequest.fromXml(Set.empty)(deleteQueryRequest).get.isInstanceOf[DeleteQueryRequest] should be(true)
  }
}