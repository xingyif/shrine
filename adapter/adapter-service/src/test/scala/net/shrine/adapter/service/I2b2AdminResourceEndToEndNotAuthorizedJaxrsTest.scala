package net.shrine.adapter.service

import org.junit.Test
import net.shrine.adapter.HasI2b2AdminDao
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.client.Poster
import net.shrine.protocol.ReadI2b2AdminQueryingUsersRequest
import net.shrine.protocol.I2b2AdminReadQueryDefinitionRequest
import net.shrine.protocol.RunHeldQueryRequest

/**
 * @author clint
 * @date Apr 24, 2013
 */
final class I2b2AdminResourceEndToEndNotAuthorizedJaxrsTest extends AbstractI2b2AdminResourceJaxrsTest with HasI2b2AdminDao {
  
  private[this] val dummyUrl = "http://example.com"

  override def makeHandler = new I2b2AdminService(dao, i2b2AdminDao, Poster(dummyUrl, NeverAuthenticatesMockPmHttpClient), null)
  
  @Test
  def testReadQueryDefinitionNotAuthorized = afterLoadingTestData {
    //Query for a query def we know is present
    val req = I2b2AdminReadQueryDefinitionRequest(projectId, waitTime, authn, networkQueryId1)
    
    val resp = adminClient.readQueryDefinition(req)
    
    resp.isInstanceOf[ErrorResponse] should be(true)
  }
  
  import ReadI2b2AdminPreviousQueriesRequest.Username._

  @Test
  def testReadI2b2AdminPreviousQueriesNotAuthorized = afterLoadingTestData {
    //Query for a queries we know are present
    val req = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), queryName1, 10, None)
    
    val resp = adminClient.readI2b2AdminPreviousQueries(req)
    
    resp.isInstanceOf[ErrorResponse] should be(true)
  }
  
  @Test
  def testReadI2b2QueryingUsersNotAuthorized = afterLoadingTestData {
    val req = ReadI2b2AdminQueryingUsersRequest(projectId, waitTime, authn, "foo")
    
    val resp = adminClient.readI2b2AdminQueryingUsers(req)
    
    resp.isInstanceOf[ErrorResponse] should be(true)
  }
  
  @Test
  def testRunHeldQueryNotAuthorized = afterLoadingTestData {
    val req = RunHeldQueryRequest(projectId, waitTime, authn, 12345L)
    
    val resp = adminClient.runHeldQuery(req)
    
    resp.isInstanceOf[ErrorResponse] should be(true)
  }
}
