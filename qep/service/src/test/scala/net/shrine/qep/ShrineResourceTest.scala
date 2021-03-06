package net.shrine.qep

import net.shrine.util.ShouldMatchersForJUnit
import org.scalatest.mock.EasyMockSugar
import org.easymock.EasyMock.{ eq => isEqualTo, expect => invoke, reportMatcher }
import net.shrine.protocol._
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import org.easymock.IArgumentMatcher
import org.easymock.internal.ArgumentToString
import org.junit.Test
import net.shrine.util.XmlDateHelper
import org.junit.Before

/**
 * @author Clint Gilbert
 * @since 9/13/2011
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class ShrineResourceTest extends ShouldMatchersForJUnit with EasyMockSugar {

  private var handler: ShrineRequestHandler = _
  private var resource: ShrineResource = _

  private val projectId = "projectId"
  private val authenticationInfo = new AuthenticationInfo("domain", "username", new Credential("secret", true))
  private val userId = "userId"

  private val shouldBroadcast = true
    
  @Before
  def setUp(): Unit = {
    handler = mock[ShrineRequestHandler]
    resource = new ShrineResource(handler)
  }

  import ShrineResource.waitTime
  
  @Test
  def testReadApprovedQueryTopics {
    val expectedRequest = ReadApprovedQueryTopicsRequest(projectId, waitTime, authenticationInfo, userId)
    val expectedResponse = ReadApprovedQueryTopicsResponse(Seq(ApprovedTopic(123L, "foo")))

    setExpectations(_.readApprovedQueryTopics, expectedRequest, expectedResponse)

    execute {
      resource.readApprovedQueryTopics(projectId, authenticationInfo, userId, shouldBroadcast)
    }
  }

  @Test
  def testReadPreviousQueries {
    def doTestReadPreviousQueries(userId: String, fetchSize: Int, expectedFetchSize: Int) {
      //Call setUp again create a new mock and new ShrinResource;
      //each pair of expecting/whenExecuting calls needs a fresh mock.
      this.setUp()

      val expectedRequest = ReadPreviousQueriesRequest(projectId, waitTime, authenticationInfo, userId, expectedFetchSize)
      val expectedResponse = ReadPreviousQueriesResponse(Seq.empty)

      setExpectations(_.readPreviousQueries, expectedRequest, expectedResponse)

      execute {
        resource.readPreviousQueries(projectId, authenticationInfo, userId, fetchSize, shouldBroadcast)
      }
    }

    doTestReadPreviousQueries(authenticationInfo.username, -100, -100)
    doTestReadPreviousQueries(authenticationInfo.username, 0, 20)
    doTestReadPreviousQueries(authenticationInfo.username, 1, 1)
    doTestReadPreviousQueries(authenticationInfo.username, 100, 100)
  }

  @Test
  def testRunQuery {
    val outputTypes = ResultOutputType.values.toSet
    val queryDef = QueryDefinition("foo", Term("nuh"))
    val topicId = Some("topicId")
    val topicName = Some("topicName")
    val networkQueryId: Long = 999L


    val expectedRequest = RunQueryRequest(projectId, waitTime, authenticationInfo, networkQueryId, topicId, topicName, outputTypes, queryDef)
    val expectedResponse = RunQueryResponse(networkQueryId, null, "userId", "groupId", queryDef, 0L, QueryResult(1L, 0L, Some(ResultOutputType.PATIENT_COUNT_XML), 123L, None, None, None, QueryResult.StatusType.Finished, None))

    def isEqualToExceptForQueryId(expected: RunQueryRequest): RunQueryRequest = {
      reportMatcher(new IArgumentMatcher {
        override def matches(argument: AnyRef): Boolean = {
          argument.isInstanceOf[RunQueryRequest] && {
            val actual = argument.asInstanceOf[RunQueryRequest]
            
            //Everything *but* queryId, which is randomly generated by ShrineResource :\
            actual.authn == expected.authn &&
            actual.outputTypes == expected.outputTypes &&
            actual.projectId == expected.projectId &&
            actual.queryDefinition == expected.queryDefinition &&
            actual.requestType == expected.requestType &&
            actual.topicId == expected.topicId &&
            actual.waitTime == expected.waitTime
          }
        }
        
        override def appendTo(buffer: StringBuffer): Unit = ArgumentToString.appendArgument(expected, buffer)
      })
      
      null
    }
    
    expecting {
      invoke(handler.runQuery(isEqualToExceptForQueryId(expectedRequest), isEqualTo(shouldBroadcast))).andReturn(expectedResponse)
    }

    execute {
      resource.runQuery(projectId, authenticationInfo, topicId.get, topicName.get, new OutputTypeSet(outputTypes), queryDef.toXmlString, shouldBroadcast)
    }
  }

  @Test
  def testReadQueryInstances {
    val queryId = 999L

    val expectedRequest = ReadQueryInstancesRequest(projectId, waitTime, authenticationInfo, queryId)
    val expectedResponse = ReadQueryInstancesResponse(queryId, "userId", "groupId", Seq.empty)

    setExpectations(_.readQueryInstances, expectedRequest, expectedResponse)
    
    execute {
      resource.readQueryInstances(projectId, authenticationInfo, queryId, shouldBroadcast)
    }
  }
  
  @Test
  def testReadInstanceResults {
    val instanceId = 123456789L
    
    val expectedRequest = ReadInstanceResultsRequest(projectId, waitTime, authenticationInfo, instanceId)
    val expectedResponse = AggregatedReadInstanceResultsResponse(instanceId, Seq.empty)
    
    setExpectations(_.readInstanceResults, expectedRequest, expectedResponse)
    
    execute {
      resource.readInstanceResults(projectId, authenticationInfo, instanceId, shouldBroadcast)
    }
  }
  @Test
  def testReadQueryDefinition {
    val queryId = 123456789L
    
    val expectedRequest = ReadQueryDefinitionRequest(projectId, waitTime, authenticationInfo, queryId)
    
    val expectedResponse = ReadQueryDefinitionResponse(queryId, "name", "userId", XmlDateHelper.now, "<foo/>")
    
    setExpectations(_.readQueryDefinition, expectedRequest, expectedResponse)
    
    execute {
      resource.readQueryDefinition(projectId, authenticationInfo, queryId, shouldBroadcast)
    }
  }

  @Test
  def testDeleteQuery {
    val queryId = 123456789L
    
    val expectedRequest = DeleteQueryRequest(projectId, waitTime, authenticationInfo, queryId)
    
    val expectedResponse = DeleteQueryResponse(queryId)
    
    setExpectations(_.deleteQuery, expectedRequest, expectedResponse)
    
    execute {
      resource.deleteQuery(projectId, authenticationInfo, queryId, shouldBroadcast)
    }
  }

  @Test
  def testRenameQuery {
    val queryId = 123456789L
    val queryName = "asjkdhkahsf"
    
    val expectedRequest = RenameQueryRequest(projectId, waitTime, authenticationInfo, queryId, queryName)
    
    val expectedResponse = RenameQueryResponse(queryId, queryName)
    
    setExpectations(_.renameQuery, expectedRequest, expectedResponse)
    
    execute {
      resource.renameQuery(projectId, authenticationInfo, queryId, queryName, shouldBroadcast)
    }
  }
  
  def testReadQueryResult {
    val queryId = 123456789L
    
    val expectedRequest = ReadQueryResultRequest(projectId, waitTime, authenticationInfo, queryId)
    
    val expectedResponse = AggregatedReadQueryResultResponse(queryId, Seq.empty)
    
    setExpectations(_.readQueryResult, expectedRequest, expectedResponse)
    
    execute {
      resource.readQueryResults(projectId, authenticationInfo, queryId, shouldBroadcast)
    }
  }
  
  private def execute(f: => Unit) = whenExecuting(handler)(f)

  private def setExpectations[Req <: BaseShrineRequest, Resp <: BaseShrineResponse](handlerMethod: ShrineRequestHandler => (Req, Boolean) => BaseShrineResponse, expectedRequest: Req, expectedResponse: Resp) {
    expecting {
      invoke(handlerMethod(handler)(isEqualTo(expectedRequest), isEqualTo(shouldBroadcast))).andReturn(expectedResponse)
    }
  }
}