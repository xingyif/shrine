package net.shrine.adapter.service

import org.junit.Test
import net.shrine.adapter.HasI2b2AdminDao
import net.shrine.protocol.{HiveCredentials, ReadI2b2AdminPreviousQueriesRequest, ReadI2b2AdminQueryingUsersRequest, ReadI2b2AdminQueryingUsersResponse, I2b2AdminUserWithRole, ErrorResponse, RunHeldQueryRequest, RunQueryResponse, RunQueryRequest, ResultOutputType, QueryResult, BroadcastMessage, AuthenticationInfo, Credential, DefaultBreakdownResultOutputTypes}
import net.shrine.client.Poster
import net.shrine.adapter.RunQueryAdapter
import net.shrine.adapter.translators.QueryDefinitionTranslator
import net.shrine.adapter.translators.ExpressionTranslator
import net.shrine.client.HttpClient
import net.shrine.client.HttpResponse
import net.shrine.protocol.query.Term
import scala.util.Success
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.query.QueryDefinition

/**
 * @author clint
 * @since Apr 12, 2013
 *
 * NB: Ideally we would extend JerseyTest here, but since we have to extend AbstractDependencyInjectionSpringContextTests,
 * we get into a diamond-problem when extending JerseyTest as well, even when both of them are extended by shim traits.
 *
 * We work around this issue by mising in JerseyTestCOmponent, which brings in a JerseyTest by composition, and ensures
 * that it is set up and torn down properly.
 */
final class I2b2AdminResourceEndToEndJaxrsTest extends AbstractI2b2AdminResourceJaxrsTest with HasI2b2AdminDao {

  private[this] val dummyUrl = "http://example.com"
  
  private[this] val dummyText = "This is dummy text"
    
  private[this] val dummyMasterId = 873456L
  private[this] val dummyInstanceId = 99L
  private[this] val dummyResultId = 42L
  private[this] val dummySetSize = 12345L
  private[this] val networkAuthn = AuthenticationInfo("network-domain", "network-username", Credential("network-password", false))

  private lazy val runQueryAdapter: RunQueryAdapter = {
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(Map("n1" -> Set("l1")))) 
    
    val poster = new Poster(dummyUrl, new HttpClient {
      override def post(input: String, url: String): HttpResponse = {
        RunQueryRequest.fromI2b2String(DefaultBreakdownResultOutputTypes.toSet)(input) match {
          case Success(req) => {
            val queryResult = QueryResult(dummyResultId, dummyInstanceId, Some(ResultOutputType.PATIENT_COUNT_XML), dummySetSize, Some(XmlDateHelper.now), Some(XmlDateHelper.now), Some("desc"), QueryResult.StatusType.Finished, Some("status"))
            
            val resp = RunQueryResponse(dummyMasterId, XmlDateHelper.now, networkAuthn.username, networkAuthn.domain, req.queryDefinition, 123L, queryResult)
            
            HttpResponse.ok(resp.toI2b2String)
          }
          case _ => ???
        }
      }
    })
    
    RunQueryAdapter(
      poster = poster,
      dao = dao,
      hiveCredentials = HiveCredentials("d", "u", "pwd", "pid"),
      conceptTranslator = translator,
      adapterLockoutAttemptsThreshold = 1000,
      doObfuscation = false,
      runQueriesImmediately = true,
      breakdownTypes = DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false,
      botCountTimeThresholds = Seq.empty
    )
  }
  
  override def makeHandler = new I2b2AdminService(dao, i2b2AdminDao, Poster(dummyUrl, AlwaysAuthenticatesMockPmHttpClient), runQueryAdapter)
  
  @Test
  def testReadQueryDefinition = afterLoadingTestData {
    doTestReadQueryDefinition(networkQueryId1, Some((queryName1, queryDef1)))
  }
  
  @Test
  def testReadQueryDefinitionUnknownQueryId = afterLoadingTestData {
    doTestReadQueryDefinition(87134682364L, None)
  }
  
  import ReadI2b2AdminPreviousQueriesRequest.{Username, Category, SortOrder}
  import Username._
  
  @Test
  def testReadI2b2AdminPreviousQueries = afterLoadingTestData {
    val searchString = queryName1
    val maxResults = 123
    val sortOrder = ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending
    val categoryToSearchWithin = ReadI2b2AdminPreviousQueriesRequest.Category.All
    val searchStrategy = ReadI2b2AdminPreviousQueriesRequest.Strategy.Exact

    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, All, searchString, maxResults, None, sortOrder, searchStrategy, categoryToSearchWithin)

    doTestReadI2b2AdminPreviousQueries(request, Seq(queryMaster1))
  }
  
  @Test
  def testReadI2b2AdminPreviousQueriesNoResultsExpected = afterLoadingTestData {
    //A request that won't return anything
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, All, "askjdhakfgkafgkasf", 123, None)
    
    doTestReadI2b2AdminPreviousQueries(request, Nil)
  }
  
  @Test
  def testReadI2b2AdminPreviousQueriesExcludeUser: Unit = afterLoadingTestData {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Except(authn2.username), "", 10, None)

    doTestReadI2b2AdminPreviousQueries(request, Seq(queryMaster2, queryMaster1))
  }
  
  @Test
  def testReadI2b2AdminPreviousQueriesOnlyFlagged: Unit = afterLoadingTestData {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, All, "", 10, None, categoryToSearchWithin = Category.Flagged)

    doTestReadI2b2AdminPreviousQueries(request, Seq(queryMaster4, queryMaster1))
  }
  
  @Test
  def testReadPreviousQueriesOnlyFlaggedExcludingUser: Unit = afterLoadingTestData {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Except(authn.username), "", 10, None, categoryToSearchWithin = Category.Flagged)

    doTestReadI2b2AdminPreviousQueries(request, Seq(queryMaster4))
  }
  
  @Test
  def testReadPreviousQueriesExcludingUserWithSearchString: Unit = afterLoadingTestData {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, All, queryName1, 10, None, categoryToSearchWithin = Category.Flagged)

    doTestReadI2b2AdminPreviousQueries(request, Seq(queryMaster1))
  }
  
  @Test
  def testReadI2b2QueryingUsers = afterLoadingTestData {
    val request = ReadI2b2AdminQueryingUsersRequest(projectId, waitTime, authn, "foo")

    val ReadI2b2AdminQueryingUsersResponse(users) = adminClient.readI2b2AdminQueryingUsers(request)
    
    users.toSet should equal(Set(I2b2AdminUserWithRole(shrineProjectId, authn.username, "USER"), I2b2AdminUserWithRole(shrineProjectId, authn2.username, "USER")))
  }
  
  @Test
  def testReadI2b2QueryingUsersNoResultsExpected = afterCreatingTables {
    val request = ReadI2b2AdminQueryingUsersRequest(projectId, waitTime, authn, "foo")

    val ReadI2b2AdminQueryingUsersResponse(users) = adminClient.readI2b2AdminQueryingUsers(request)
    
    //DB is empty, so no users will be returned
    users should equal(Nil)
  }
  
  @Test
  def testRunHeldQueryUnknownQuery = afterCreatingTables {
    val request = RunHeldQueryRequest(projectId, waitTime, authn, 12345L)

    val resp = adminClient.runHeldQuery(request)
    
    resp.isInstanceOf[ErrorResponse] should be(true)
  }
  
  @Test
  def testRunHeldQueryKnownQuery = afterCreatingTables {
    val networkQueryId = 12345L
    
    val request = RunHeldQueryRequest(projectId, waitTime, authn, networkQueryId)
    
    val queryName = "aslkdjasljkd"
    val queryExpr = Term("n1")
    
    val runQueryReq = RunQueryRequest(projectId, waitTime, authn, networkQueryId, None, None, Set(ResultOutputType.PATIENT_COUNT_XML), QueryDefinition(queryName, queryExpr))

    runQueryAdapter.copy(runQueriesImmediately = false).processRequest(BroadcastMessage(networkAuthn, runQueryReq))
    
    val resp = adminClient.runHeldQuery(request)
    
    val runQueryResp = resp.asInstanceOf[RunQueryResponse]
    
    runQueryResp.createDate should not be(null)
    runQueryResp.groupId should be(networkAuthn.domain)
    runQueryResp.userId should equal(networkAuthn.username)
    runQueryResp.queryId should equal(dummyMasterId)
    runQueryResp.singleNodeResult.setSize should equal(dummySetSize) 
    runQueryResp.singleNodeResult.resultType should equal(Some(ResultOutputType.PATIENT_COUNT_XML)) //TODO
    runQueryResp.requestXml.name should equal(queryName) 
    runQueryResp.requestXml.expr.get should equal(Term("l1"))
  }
}
