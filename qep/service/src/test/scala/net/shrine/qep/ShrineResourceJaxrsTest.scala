package net.shrine.qep

import com.sun.jersey.api.client.UniformInterfaceException
import net.shrine.client.JerseyShrineClient
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.protocol.{AggregatedReadInstanceResultsResponse, AggregatedReadQueryResultResponse, AggregatedReadTranslatedQueryDefinitionResponse, AggregatedRunQueryResponse, ApprovedTopic, AuthenticationInfo, BaseShrineResponse, Credential, DefaultBreakdownResultOutputTypes, DeleteQueryRequest, DeleteQueryResponse, FlagQueryRequest, FlagQueryResponse, QueryResult, ReadApprovedQueryTopicsRequest, ReadApprovedQueryTopicsResponse, ReadInstanceResultsRequest, ReadPreviousQueriesRequest, ReadPreviousQueriesResponse, ReadQueryDefinitionRequest, ReadQueryDefinitionResponse, ReadQueryInstancesRequest, ReadQueryInstancesResponse, ReadQueryResultRequest, ReadTranslatedQueryDefinitionRequest, RenameQueryRequest, RenameQueryResponse, RequestType, ResultOutputType, RunQueryRequest, ShrineRequest, ShrineRequestHandler, UnFlagQueryRequest, UnFlagQueryResponse}
import net.shrine.protocol.query.{QueryDefinition, Term}
import net.shrine.util.{AbstractPortSearchingJerseyTest, JerseyAppDescriptor, ShouldMatchersForJUnit, XmlDateHelper}
import org.junit.{After, Before, Test}

/**
 *
 * @author Clint Gilbert
 * @since Sep 14, 2011
 *
 * @see http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * Starts a ShrineResource in an embedded HTTP server, sends requests to it, then verifies that the requests don't fail,
 * and that the parameters made it from the client to the ShrineResource successfully.  Uses a mock ShrineRequestHandler, so
 * it doesn't test that correct values are returned by the ShrineResource.
 */
final class ShrineResourceJaxrsTest extends AbstractPortSearchingJerseyTest with ShouldMatchersForJUnit {
  private val projectId = "some-project-id"

  private val topicId = "some-topic-id"

  private val userId = "some-user-id"

  private val authenticationInfo = AuthenticationInfo("some-domain", userId, new Credential("some-val", false))

  private val shrineClient = new JerseyShrineClient(resource.getURI.toString, projectId, authenticationInfo, DefaultBreakdownResultOutputTypes.toSet, AcceptAllCerts)

  /**
   * We invoked the no-arg superclass constructor, so we must override configure() to provide an AppDescriptor
   * That tells Jersey to instantiate and expose ShrineResource
   */
  override def configure = JerseyAppDescriptor.thatCreates(ShrineResource).using(MockShrineRequestHandler)
  
  @Before
  override def setUp(): Unit = super.setUp()
  
  @After
  override def tearDown(): Unit = super.tearDown()
  
  @Test
  def testReadApprovedQueryTopics {
    val response = shrineClient.readApprovedQueryTopics(userId)

    response should not(be(null))

    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.readQueryDefinitionParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)

    val param = MockShrineRequestHandler.readApprovedQueryTopicsParam

    validateCachedParam(param, RequestType.SheriffRequest)

    param.userId should equal(userId)
  }

  @Test
  def testReadPreviousQueries = resetMockThen {
    val fetchSize = 123

    val response = shrineClient.readPreviousQueries(userId, fetchSize)

    response should not(be(null))

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.readQueryDefinitionParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)

    val param = MockShrineRequestHandler.readPreviousQueriesParam

    validateCachedParam(param, RequestType.UserRequest)

    param.fetchSize should equal(fetchSize)
    param.userId should equal(userId)
  }

  @Test
  def testReadPreviousQueriesUsernameMismatch = resetMockThen {
    intercept[UniformInterfaceException] {
      shrineClient.readPreviousQueries("foo", 123)
    }

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.readQueryDefinitionParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
  }

  @Test
  def testRunQuery = resetMockThen {

    val queryDef = QueryDefinition("foo", Term("nuh"))

    def doTestRunQueryResponse(response: AggregatedRunQueryResponse, expectedOutputTypes: Set[ResultOutputType]) {

      response should not(be(null))

      MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
      MockShrineRequestHandler.readPreviousQueriesParam should be(null)
      MockShrineRequestHandler.readQueryInstancesParam should be(null)
      MockShrineRequestHandler.readInstanceResultsParam should be(null)
      MockShrineRequestHandler.readQueryDefinitionParam should be(null)
      MockShrineRequestHandler.deleteQueryParam should be(null)
      MockShrineRequestHandler.renameQueryParam should be(null)
      MockShrineRequestHandler.readQueryResultParam should be(null)

      val param = MockShrineRequestHandler.runQueryParam

      validateCachedParam(param, RequestType.QueryDefinitionRequest)

      param.outputTypes should equal(expectedOutputTypes)
      param.queryDefinition should equal(queryDef)
      param.topicId should equal(Some(topicId))
    }

    def doTestRunQuery(outputTypes: Set[ResultOutputType]) {
      val responseScalaSet = shrineClient.runQuery(topicId, outputTypes, queryDef)

      doTestRunQueryResponse(responseScalaSet, outputTypes)

      val responseJavaSet = shrineClient.runQuery(topicId, outputTypes, queryDef)

      doTestRunQueryResponse(responseJavaSet, outputTypes)
    }

    Seq(ResultOutputType.values.toSet,
      Set(ResultOutputType.PATIENT_COUNT_XML),
      Set(ResultOutputType.PATIENTSET),
      Set.empty[ResultOutputType]).foreach(doTestRunQuery)
  }

  @Test
  def testReadQueryInstances = resetMockThen {
    val queryId = 123L

    val response = shrineClient.readQueryInstances(queryId)

    response should not(be(null))

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.readQueryDefinitionParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)

    val param = MockShrineRequestHandler.readQueryInstancesParam

    validateCachedParam(param, RequestType.MasterRequest)

    param.queryId should equal(queryId)
  }

  @Test
  def testReadInstanceResults = resetMockThen {
    val shrineNetworkQueryId = 98765L

    val response = shrineClient.readInstanceResults(shrineNetworkQueryId)

    response should not(be(null))

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readQueryDefinitionParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)

    val param = MockShrineRequestHandler.readInstanceResultsParam

    validateCachedParam(param, RequestType.InstanceRequest)

    param.shrineNetworkQueryId should equal(shrineNetworkQueryId)
  }

  @Test
  def testReadQueryDefinition = resetMockThen {
    val queryId = 3789894L

    val response = shrineClient.readQueryDefinition(queryId)

    response should not(be(null))

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)

    val param = MockShrineRequestHandler.readQueryDefinitionParam

    validateCachedParam(param, RequestType.GetRequestXml)

    param.queryId should equal(queryId)
  }

  @Test
  def testDeleteQuery = resetMockThen {
    val queryId = 3789894L

    val response = shrineClient.deleteQuery(queryId)

    response should not(be(null))

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)

    val param = MockShrineRequestHandler.deleteQueryParam

    validateCachedParam(param, RequestType.MasterDeleteRequest)

    param.queryId should equal(queryId)
  }

  @Test
  def testRenameQuery = resetMockThen {
    val queryId = 3789894L
    val queryName = "aslkfhkasfh"

    val response = shrineClient.renameQuery(queryId, queryName)

    response should not(be(null))

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)

    val param = MockShrineRequestHandler.renameQueryParam

    validateCachedParam(param, RequestType.MasterRenameRequest)

    param.queryId should equal(queryId)
    param.queryName should equal(queryName)
  }

  @Test
  def testReadQueryResult = resetMockThen {
    val queryId = 3789894L

    val response = shrineClient.readQueryResult(queryId)

    response should not(be(null))

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)

    val param = MockShrineRequestHandler.readQueryResultParam

    MockShrineRequestHandler.shouldBroadcastParam should be(true)
    param should not(be(null))
    param.projectId should equal(projectId)
    param.authn should equal(authenticationInfo)
    param.requestType should equal(RequestType.GetQueryResult)
    param.waitTime should equal(ShrineResource.waitTime)

    param.queryId should equal(queryId)
  }
  
  @Test
  def testReadTranslatedQueryDefinition = resetMockThen {
    val queryDef = QueryDefinition("foo", Term("network"))
    
    val response = shrineClient.readTranslatedQueryDefinition(queryDef)

    response should not(be(null))

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)

    val param = MockShrineRequestHandler.readTranslatedQueryDefinitionParam

    MockShrineRequestHandler.shouldBroadcastParam should be(true)
    param should not(be(null))
    param.authn should equal(authenticationInfo)
    param.requestType should equal(RequestType.ReadTranslatedQueryDefinitionRequest)
    
    param.queryDef should equal(queryDef)
  }
  
  @Test
  def testFlagQuery: Unit = resetMockThen {
    val queryId = 12345L
    val message = "laskfhdklsjfhksdf"
    val response = shrineClient.flagQuery(queryId, Some(message), true)

    response should equal(FlagQueryResponse)

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)
    MockShrineRequestHandler.readTranslatedQueryDefinitionParam should be(null)

    val param = MockShrineRequestHandler.flagQueryRequestParam

    MockShrineRequestHandler.shouldBroadcastParam should be(true)
    
    param should not(be(null))
    param.authn should equal(authenticationInfo)
    param.requestType should equal(RequestType.FlagQueryRequest)
    
    param.networkQueryId should equal(queryId)
    param.message should equal(Some(message))
  }
  
  @Test
  def testUnFlagQuery: Unit = resetMockThen {
    val queryId = 12345L
    
    val response = shrineClient.unFlagQuery(queryId, true)

    response should equal(UnFlagQueryResponse)

    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)
    MockShrineRequestHandler.readTranslatedQueryDefinitionParam should be(null)
    MockShrineRequestHandler.flagQueryRequestParam should be(null)

    val param = MockShrineRequestHandler.unFlagQueryRequestParam

    MockShrineRequestHandler.shouldBroadcastParam should be(true)
    
    param should not(be(null))
    param.authn should equal(authenticationInfo)
    param.requestType should equal(RequestType.UnFlagQueryRequest)
    
    param.networkQueryId should equal(queryId)
  }

  private def validateCachedParam(param: ShrineRequest, expectedRequestType: RequestType) {
    MockShrineRequestHandler.shouldBroadcastParam should be(true)
    param should not(be(null))
    param.projectId should equal(projectId)
    param.authn should equal(authenticationInfo)
    param.requestType should equal(expectedRequestType)
    param.waitTime should equal(ShrineResource.waitTime)
  }

  private def resetMockThen(body: => Any) {
    MockShrineRequestHandler.reset()

    //(Healthy?) paranoia
    MockShrineRequestHandler.readApprovedQueryTopicsParam should be(null)
    MockShrineRequestHandler.readPreviousQueriesParam should be(null)
    MockShrineRequestHandler.runQueryParam should be(null)
    MockShrineRequestHandler.readQueryInstancesParam should be(null)
    MockShrineRequestHandler.readInstanceResultsParam should be(null)
    MockShrineRequestHandler.readQueryDefinitionParam should be(null)
    MockShrineRequestHandler.deleteQueryParam should be(null)
    MockShrineRequestHandler.renameQueryParam should be(null)
    MockShrineRequestHandler.readQueryResultParam should be(null)
    MockShrineRequestHandler.flagQueryRequestParam should be(null)
    MockShrineRequestHandler.unFlagQueryRequestParam should be(null)
    MockShrineRequestHandler.readTranslatedQueryDefinitionParam should be(null)

    body
  }

  /**
   * Mock ShrineRequestHandler; stores passed parameters for later inspection.
   * Private, since this is (basically) the enclosing test class's state
   */
  private object MockShrineRequestHandler extends ShrineRequestHandler {
    var shouldBroadcastParam = false

    var readApprovedQueryTopicsParam: ReadApprovedQueryTopicsRequest = _
    var readPreviousQueriesParam: ReadPreviousQueriesRequest = _
    var runQueryParam: RunQueryRequest = _
    var readQueryInstancesParam: ReadQueryInstancesRequest = _
    var readInstanceResultsParam: ReadInstanceResultsRequest = _
    var readQueryDefinitionParam: ReadQueryDefinitionRequest = _
    var deleteQueryParam: DeleteQueryRequest = _
    var renameQueryParam: RenameQueryRequest = _
    var readQueryResultParam: ReadQueryResultRequest = _
    var readTranslatedQueryDefinitionParam: ReadTranslatedQueryDefinitionRequest = _
    var flagQueryRequestParam: FlagQueryRequest = _
    var unFlagQueryRequestParam: UnFlagQueryRequest = _

    def reset() {
      shouldBroadcastParam = false
      readApprovedQueryTopicsParam = null
      readPreviousQueriesParam = null
      runQueryParam = null
      readQueryInstancesParam = null
      readInstanceResultsParam = null
      readQueryDefinitionParam = null
      deleteQueryParam = null
      renameQueryParam = null
      readQueryResultParam = null
      readTranslatedQueryDefinitionParam = null
      flagQueryRequestParam = null
      unFlagQueryRequestParam = null
    }

    import XmlDateHelper.now

    private def setShouldBroadcastAndThen(shouldBroadcast: Boolean)(f: => BaseShrineResponse): BaseShrineResponse = {
      try { f } finally {
        shouldBroadcastParam = shouldBroadcast
      }
    }

    override def readApprovedQueryTopics(request: ReadApprovedQueryTopicsRequest, shouldBroadcast: Boolean): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      readApprovedQueryTopicsParam = request

      ReadApprovedQueryTopicsResponse(Seq(new ApprovedTopic(123L, "some topic")))
    }

    override def readPreviousQueries(request: ReadPreviousQueriesRequest, shouldBroadcast: Boolean): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      readPreviousQueriesParam = request

      ReadPreviousQueriesResponse(Seq.empty)
    }

    override def readQueryInstances(request: ReadQueryInstancesRequest, shouldBroadcast: Boolean): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      readQueryInstancesParam = request

      ReadQueryInstancesResponse(999L, "userId", "groupId", Seq.empty)
    }

    override def readInstanceResults(request: ReadInstanceResultsRequest, shouldBroadcast: Boolean): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      readInstanceResultsParam = request

      AggregatedReadInstanceResultsResponse(1337L, Seq(new QueryResult(123L, 1337L, Some(ResultOutputType.PATIENT_COUNT_XML), 789L, None, None, Some("description"), QueryResult.StatusType.Finished, Some("statusMessage"))))
    }

    override def readQueryDefinition(request: ReadQueryDefinitionRequest, shouldBroadcast: Boolean): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      readQueryDefinitionParam = request

      ReadQueryDefinitionResponse(87456L, "name", "userId", now, "<foo/>")
    }

    override def runQuery(request: RunQueryRequest, shouldBroadcast: Boolean): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      runQueryParam = request

      AggregatedRunQueryResponse(123L, now, "userId", "groupId", request.queryDefinition, 456L, Seq.empty)
    }

    override def deleteQuery(request: DeleteQueryRequest, shouldBroadcast: Boolean): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      deleteQueryParam = request

      DeleteQueryResponse(56834756L)
    }

    override def renameQuery(request: RenameQueryRequest, shouldBroadcast: Boolean): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      renameQueryParam = request

      RenameQueryResponse(873468L, "some-name")
    }

    override def readQueryResult(request: ReadQueryResultRequest, shouldBroadcast: Boolean): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      readQueryResultParam = request

      AggregatedReadQueryResultResponse(1234567890L, Seq.empty)
    }
    
    override def readTranslatedQueryDefinition(request: ReadTranslatedQueryDefinitionRequest, shouldBroadcast: Boolean = true): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      readTranslatedQueryDefinitionParam = request

      AggregatedReadTranslatedQueryDefinitionResponse(Seq.empty)
    }
    
    override def flagQuery(request: FlagQueryRequest, shouldBroadcast: Boolean = true): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      flagQueryRequestParam = request
      
      FlagQueryResponse
    }
    
    override def unFlagQuery(request: UnFlagQueryRequest, shouldBroadcast: Boolean = true): BaseShrineResponse = setShouldBroadcastAndThen(shouldBroadcast) {
      unFlagQueryRequestParam = request
      
      UnFlagQueryResponse
    }
  }
}