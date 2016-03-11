package net.shrine.adapter.service

import com.sun.jersey.test.framework.{AppDescriptor, JerseyTest}
import net.shrine.adapter.service.I2b2AdminResourceJaxrsTest._
import net.shrine.client.JerseyHttpClient
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.problem.TestProblem
import net.shrine.protocol.{AbstractReadQueryDefinitionRequest, AuthenticationInfo, Credential, DefaultBreakdownResultOutputTypes, I2b2AdminReadQueryDefinitionRequest, I2b2AdminRequestHandler, I2b2AdminUserWithRole, QueryMaster, QueryResult, ReadI2b2AdminPreviousQueriesRequest, ReadI2b2AdminQueryingUsersRequest, ReadI2b2AdminQueryingUsersResponse, ReadPreviousQueriesResponse, ReadQueryDefinitionResponse, RunHeldQueryRequest, RunQueryResponse, ShrineResponse}
import net.shrine.protocol.query.{QueryDefinition, Term}
import net.shrine.util.{JerseyAppDescriptor, ShouldMatchersForJUnit, XmlDateHelper, XmlUtil}
import org.junit.{After, Before, Test}

import scala.xml.XML

/**
 * @author clint
 * @since Apr 10, 2013
 */
//noinspection ScalaUnnecessaryParentheses
final class I2b2AdminResourceJaxrsTest extends JerseyTest with ShouldMatchersForJUnit {
  
  var handler: MockShrineRequestHandler = _
  
  def resourceUrl = resource.getURI.toString + "i2b2/admin/request"
  
  override def configure: AppDescriptor = {
    JerseyAppDescriptor.thatCreates((h: I2b2AdminRequestHandler) => I2b2AdminResource(h, DefaultBreakdownResultOutputTypes.toSet)).using { 
      handler = new MockShrineRequestHandler
      
      handler
    }
  }
  
  @Before
  override def setUp(): Unit = super.setUp()
  
  @After
  override def tearDown(): Unit = super.tearDown()
  
  import scala.concurrent.duration._
  
  def adminClient = I2b2AdminClient(resourceUrl, new JerseyHttpClient(AcceptAllCerts, 5.minutes))
  
  @Test
  def testReadQueryDefinition() {
    val queryId = 987654321L
    
    val request = I2b2AdminReadQueryDefinitionRequest(projectId, waitTime, authn, queryId)
    
    val currentHandler = handler
    
    val response = adminClient.readQueryDefinition(request).asInstanceOf[ReadQueryDefinitionResponse]
    
    response should not be(null)
    response.masterId should equal(queryId)
    response.name should equal("some-query-name")
    response.createDate should not be(null)
    response.userId should equal(authn.username)
    
    def stripNamespaces(s: String) = XmlUtil.stripNamespaces(XML.loadString(s))
    
    //NB: I'm not sure why whacky namespaces were coming back from the resource;
    //this checks that the gist of the queryDef XML makes it back.
    //TODO: revisit this
    stripNamespaces(response.queryDefinition) should equal(stripNamespaces(queryDef.toI2b2String))
    
    currentHandler.shouldBroadcastParam should be(false)
    currentHandler.readI2b2AdminPreviousQueriesParam should be(null)
    currentHandler.readQueryDefinitionParam should equal(request)
  }
  
  @Test
  def testReadI2b2AdminPreviousQueries() {
    val searchString = "asdk;laskd;lask;gdjsg"
    val maxResults = 123
    val sortOrder = ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending
    val categoryToSearchWithin = ReadI2b2AdminPreviousQueriesRequest.Category.All
    val searchStrategy = ReadI2b2AdminPreviousQueriesRequest.Strategy.Exact
    
    import ReadI2b2AdminPreviousQueriesRequest.Username._
    
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), searchString, maxResults, None, sortOrder, searchStrategy, categoryToSearchWithin)
    
    val currentHandler = handler
    
    val response = adminClient.readI2b2AdminPreviousQueries(request).asInstanceOf[ReadPreviousQueriesResponse]
    
    response should not be(null)
    response.queryMasters should equal(Seq(queryMaster))
    
    currentHandler.shouldBroadcastParam should be(false)
    currentHandler.readI2b2AdminPreviousQueriesParam should be(request)
    currentHandler.readQueryDefinitionParam should be(null)
  }
  
  @Test
  def testReadI2b2QueryingUsers() {
    val projectIdToSearchFor = "foo-project-id"
    
    val request = ReadI2b2AdminQueryingUsersRequest(projectId, waitTime, authn, projectIdToSearchFor)
    
    val currentHandler = handler
    
    val response = adminClient.readI2b2AdminQueryingUsers(request).asInstanceOf[ReadI2b2AdminQueryingUsersResponse]
    
    response should not be(null)
    response.users should equal(users)
    
    currentHandler.shouldBroadcastParam should be(false)
    currentHandler.readI2b2AdminPreviousQueriesParam should be(null)
    currentHandler.readI2b2AdminQueryingUsersParam should be(request)
    currentHandler.readQueryDefinitionParam should be(null)
  }
}

object I2b2AdminResourceJaxrsTest {
  private val queryDef = QueryDefinition("foo", Term("x"))
  
  private val userId = "some-user-id"

  private val domain = "some-domain" 
    
  private lazy val authn = new AuthenticationInfo(domain, userId, new Credential("some-val", false))
  
  private val projectId = "some-project-id"
    
  import scala.concurrent.duration._
    
  private val waitTime = 12345.milliseconds
  
  private lazy val queryMaster = QueryMaster(
    queryMasterId = "queryMasterId",
    networkQueryId = 123456789L,
    name = "name",
    userId = userId,
    groupId = domain,
    createDate = XmlDateHelper.now,
    flagged = Some(true))
  
  private lazy val users = Seq(
      I2b2AdminUserWithRole("projectId1", "joe user", "some important role"),
      I2b2AdminUserWithRole("projectId2", "jane user", "some other important role"),
      I2b2AdminUserWithRole("projectId3", "some user", "some super important role"))
  
  /**
   * Mock ShrineRequestHandler; stores passed parameters for later inspection.
   * Private, since this is (basically) the enclosing test class's state
   */
  final class MockShrineRequestHandler extends I2b2AdminRequestHandler {
    private val lock = new AnyRef
    
    def shouldBroadcastParam = lock.synchronized(_shouldBroadcastParam)
    def readQueryDefinitionParam = lock.synchronized(_readQueryDefinitionParam)
    def readI2b2AdminPreviousQueriesParam = lock.synchronized(_readI2b2AdminPreviousQueriesParam)
    def readI2b2AdminQueryingUsersParam = lock.synchronized(_readI2b2AdminQueryingUsersParam)
    def runHeldQueryParam = lock.synchronized(_runHeldQueryParam)
    
    private var _shouldBroadcastParam = false
    private var _readQueryDefinitionParam: AbstractReadQueryDefinitionRequest = _
    private var _readI2b2AdminPreviousQueriesParam: ReadI2b2AdminPreviousQueriesRequest = _
    private var _readI2b2AdminQueryingUsersParam: ReadI2b2AdminQueryingUsersRequest = _
    private var _runHeldQueryParam: RunHeldQueryRequest = _

    override def readI2b2AdminPreviousQueries(request: ReadI2b2AdminPreviousQueriesRequest, shouldBroadcast: Boolean): ShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      lock.synchronized { _readI2b2AdminPreviousQueriesParam = request }
      
      ReadPreviousQueriesResponse(Seq(queryMaster))
    }
    
    override def readI2b2AdminQueryingUsers(request: ReadI2b2AdminQueryingUsersRequest, shouldBroadcast: Boolean): ShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      lock.synchronized { _readI2b2AdminQueryingUsersParam = request }
      
      ReadI2b2AdminQueryingUsersResponse(users)
    }
    
    override def readQueryDefinition(request: I2b2AdminReadQueryDefinitionRequest, shouldBroadcast: Boolean): ShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      lock.synchronized { _readQueryDefinitionParam = request }

      ReadQueryDefinitionResponse(request.queryId, "some-query-name", request.authn.username, XmlDateHelper.now, queryDef.toI2b2String)
    }
    
    override def runHeldQuery(request: RunHeldQueryRequest, shouldBroadcast: Boolean): ShrineResponse = {
      lock.synchronized { _runHeldQueryParam = request }
      
      RunQueryResponse(request.networkQueryId, XmlDateHelper.now, request.authn.username, request.projectId, QueryDefinition("bogus", Term("placeholder")), 12345L, QueryResult.errorResult(Some("foo"), "not actually an error",TestProblem))
    }
    
    private def setShouldBroadcastAndThen(shouldBroadcast: Boolean)(f: => ShrineResponse): ShrineResponse = {
      try { f } finally {
        lock.synchronized { _shouldBroadcastParam = shouldBroadcast }
      }
    }
  }
}