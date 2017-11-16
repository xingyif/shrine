package net.shrine.adapter

import junit.framework.TestCase
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.ReadQueryResultRequest
import net.shrine.protocol.ReadQueryResultResponse
import org.junit.Test
import scala.concurrent.duration._
import net.shrine.client.Poster
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 * @author clint
 * @date Nov 7, 2012
 */
final class ReadQueryResultAdapterTest extends 
	AbstractQueryRetrievalTestCase(
	    (dao, httpClient) => new ReadQueryResultAdapter(
        Poster("", httpClient),
        AbstractQueryRetrievalTestCase.hiveCredentials,
        dao,
        true,
        DefaultBreakdownResultOutputTypes.toSet,
        collectAdapterAudit = false,
        obfuscator = Obfuscator(1,1.3,3)
      ),
	    (queryId, authn) => ReadQueryResultRequest("some-project-id", 10.seconds, authn, queryId), 
	    ReadQueryResultResponse.unapply) {
  @Test
  def testProcessInvalidRequest = doTestProcessInvalidRequest
  
  @Test
  def testProcessRequest = doTestProcessRequest
  
  @Test
  def testProcessRequestMissingQuery = doTestProcessRequestMissingQuery

  //todo turn back on SHRINE-2384  @Test
  //  def testProcessRequestIncompleteQuery = doTestProcessRequestIncompleteQuery(true)

  //todo turn back on SHRINE-2384  @Test
  //  def testProcessRequestIncompleteQueryCountResultRetrievalFails = doTestProcessRequestIncompleteQuery(false)

  //todo turn back on SHRINE-2384  @Test
  //  def testProcessRequestQueuedQuery = doTestProcessRequestQueuedQuery
}