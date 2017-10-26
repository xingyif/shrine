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
 * @since Nov 7, 2012
 */
final class ReadInstanceResultsAdapterTest extends 
	AbstractQueryRetrievalTestCase(
	    (dao, httpClient) => new ReadInstanceResultsAdapter(
        Poster("", httpClient),
        AbstractQueryRetrievalTestCase.hiveCredentials,
        dao,
        true,
        DefaultBreakdownResultOutputTypes.toSet,
        collectAdapterAudit = false,
        obfuscator = Obfuscator(1,1.3,3)
      ),
	    (queryId, authn) => ReadInstanceResultsRequest("some-project-id", 10.seconds, authn, queryId), 
	    ReadInstanceResultsResponse.unapply) {
  @Test
  def testProcessInvalidRequest = doTestProcessInvalidRequest
  
  @Test
  def testProcessRequest = doTestProcessRequest

  @Test
  def testProcessRequestMissingQuery = doTestProcessRequestMissingQuery

  //todo turn back on for SHRINE-2115 after you have a fix  @Test
//  def testProcessRequestIncompleteQuery = doTestProcessRequestIncompleteQuery(true)

  //todo turn back on for SHRINE-2115 after you have a fix  @Test
//  def testProcessRequestIncompleteQueryCountResultRetrievalFails = doTestProcessRequestIncompleteQuery(false)
  
//todo turn back on for SHRINE-2115 after you have a fix  @Test
//  def testProcessRequestQueuedQuery = doTestProcessRequestQueuedQuery
}
