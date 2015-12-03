package net.shrine.adapter.components

import net.shrine.adapter.AdapterTestHelpers
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.query.Term
import net.shrine.protocol.ReadQueryDefinitionResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.adapter.service.CanLoadTestData
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest

/**
 * @author clint
 * @date Apr 23, 2013
 */
final class QueryDefinitionsTest extends AbstractSquerylAdapterTest with AdapterTestHelpers with ShouldMatchersForJUnit with CanLoadTestData {
  private val queryDefinitions = QueryDefinitions[ReadQueryDefinitionRequest](dao)
  
  private def get = queryDefinitions.get _
  
  @Test
  def testGetQueryIsPresent = afterLoadingTestData {
    val request = ReadQueryDefinitionRequest(projectId, waitTime, authn, networkQueryId1)
    
    val resp = get(request).asInstanceOf[ReadQueryDefinitionResponse]
    
    resp should not be(null)
    resp.createDate should not be(null)
    resp.masterId should equal(networkQueryId1.toLong)
    resp.name should equal(queryName1)
    resp.queryDefinition should equal(queryDef1.toI2b2String)
    resp.userId should equal(authn.username)
  }
  
  @Test
  def testGetQueryIsNOTPresent = afterCreatingTables {
    val request = ReadQueryDefinitionRequest(projectId, waitTime, authn, queryId)
    
    val resp = get(request)
    
    resp should not be(null)
    
    resp.isInstanceOf[ErrorResponse] should be(true)
  }
}