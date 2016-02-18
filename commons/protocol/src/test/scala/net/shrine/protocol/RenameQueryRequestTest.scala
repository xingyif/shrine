package net.shrine.protocol

import org.junit.Test
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
final class RenameQueryRequestTest extends ShrineRequestValidator {
  val queryId = 2422297885846950097L
  val queryName = "Cholecystitis w@14:41:52 [3-28-2011]"

  override def messageBody = XmlUtil.stripWhitespace {
    <message_body>
		<ns4:psmheader>
			<user login={username}>{username}</user>
			<patient_set_limit>0</patient_set_limit>
			<estimated_time>0</estimated_time>
			<request_type>CRC_QRY_renameQueryMaster</request_type>
		</ns4:psmheader>
		<ns4:request xsi:type="ns4:master_rename_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
			<user_id>{username}</user_id>
			<query_master_id>{queryId}</query_master_id>
			<query_name>{queryName}</query_name>
		</ns4:request>
	</message_body>
  }

  val renameQueryRequest = XmlUtil.stripWhitespace {
      <renameQuery>
        {requestHeaderFragment}
        <queryId>{queryId}</queryId>
        <queryName>{queryName}</queryName>
      </renameQuery>
  }

  @Test
  override def testFromI2b2 {
    val translatedRequest = RenameQueryRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get
    
    validateRequestWith(translatedRequest) {
      translatedRequest.networkQueryId should equal(queryId)
      translatedRequest.queryName should equal(queryName)
    }
  }

  @Test
  override def testShrineRequestFromI2b2 {
    val shrineRequest = CrcRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get
    
    shrineRequest.isInstanceOf[RenameQueryRequest] should be(true)
  }
  
  @Test
  def testDoubleDispatchingShrineRequestFromI2b2 {
    val shrineRequest = HandleableShrineRequest.fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(request).get
    
    shrineRequest.isInstanceOf[RenameQueryRequest] should be(true)
  }

  @Test
  override def testToXml {
    RenameQueryRequest(projectId, waitTime, authn, queryId, queryName).toXml should equal(renameQueryRequest)
  }

  @Test
  override def testToI2b2 {
    RenameQueryRequest(projectId, waitTime, authn, queryId, queryName).toI2b2 should equal(request)
  }

  @Test
  override def testFromXml {
    val actual = RenameQueryRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(renameQueryRequest).get
    
    validateRequestWith(actual) {
      actual.networkQueryId should equal(queryId)
      actual.queryName should equal(queryName)
    }
  }

  @Test
  def testShrineRequestFromXml {
    ShrineRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(renameQueryRequest).get.isInstanceOf[RenameQueryRequest] should be(true)
  }
}