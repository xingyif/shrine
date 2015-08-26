package net.shrine.adapter

import scala.concurrent.duration.DurationInt
import org.junit.Test
import net.shrine.client.Poster
import net.shrine.protocol.ReadInstanceResultsRequest
import net.shrine.protocol.ReadInstanceResultsResponse
import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 * @author clint
 * @date Nov 7, 2012
 */
final class ReadInstanceResultsAdapterTest extends 
	AbstractQueryRetrievalTestCase(
	    (dao, httpClient) => new ReadInstanceResultsAdapter(
        Poster("", httpClient),
        AbstractQueryRetrievalTestCase.hiveCredentials,
        dao,
        true,
        DefaultBreakdownResultOutputTypes.toSet,
        collectAdapterAudit = false
      ),
	    (queryId, authn) => ReadInstanceResultsRequest("some-project-id", 10.seconds, authn, queryId), 
	    ReadInstanceResultsResponse.unapply) {
  @Test
  def testProcessInvalidRequest = doTestProcessInvalidRequest
  
  @Test
  def testProcessRequest = doTestProcessRequest
  
  @Test
  def testProcessRequestMissingQuery = doTestProcessRequestMissingQuery
  
  @Test
  def testProcessRequestIncompleteQuery = doTestProcessRequestIncompleteQuery(true)
  
  @Test
  def testProcessRequestIncompleteQueryCountResultRetrievalFails = doTestProcessRequestIncompleteQuery(false)
  
  @Test
  def testProcessRequestQueuedQuery = doTestProcessRequestQueuedQuery
}
