package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

import scala.xml.NodeSeq
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.util.XmlDateHelper
import java.math.BigInteger

import net.shrine.problem.TestProblem

/**
 * @author clint
 * @since Feb 24, 2014
 */
final class ShrineMessageTest extends ShouldMatchersForJUnit {
  @Test
  def testRoundTrips {
    val projectId = "salkdjksaljdkla"

    import scala.concurrent.duration._

    val waitTime: Duration = 98374L.milliseconds
    val userId = "foo-user"
    val groupId = "foo-group"
    val authn = AuthenticationInfo("blarg-domain", userId, Credential("sajkhdkjsadh", true))
    val queryId = 485794359L
    val patientSetCollId = "ksaldjksal"
    val optionsXml: NodeSeq = <request><foo>x</foo></request>
    val fetchSize = 12345
    val queryName = "saljkd;salda"
    val topicId = "saldjkasljdasdsadsadasdas"
    val topicName = "Topic Name"
    val outputTypes = ResultOutputType.nonBreakdownTypes.toSet
    val queryDefinition = QueryDefinition(queryName, Term("oiweruoiewkldfhsofi"))
    val queryDefinition2 = QueryDefinition(queryName, Term("a;slkjflfjlsdkjf"))
    val localResultId = "aoiduaojsdpaojcmsal"
    val nodeId = NodeId("foo")
    val nodeId2 = NodeId("bar")
    val queryTopicId1 = 123L
    val queryTopicId2 = 456L
    val queryTopicName1 = "nuh"
    val queryTopicName2 = "zuh"
    val shrineNetworkQueryId = 1287698235L
    val start = Some(XmlDateHelper.now)
    val end = Some(XmlDateHelper.now)
    val singleNodeResult1 = QueryResult.errorResult(Some("blarg"), "glarg",TestProblem())
    val singleNodeResult2 = QueryResult(
      42L,
      99L,
      Option(ResultOutputType.PATIENT_COUNT_XML),
      123L,
      start,
      end,
      Some("description"),
      QueryResult.StatusType.Finished,
      Some("status"))

    val param1 = ParamResponse("foo", "bar", "baz")
    val queryMaster1 = QueryMaster("kjasdh", 12345L, "name1", userId, groupId, start.get)
    val queryMaster2 = QueryMaster("skdjlhlasf", 873563L, "name2", userId, groupId, end.get)
    val queryInstance1 = QueryInstance("asd", "42", userId, groupId, start.get, end.get)
    val queryInstance2 = QueryInstance("asdasd", "99", userId, groupId, start.get, end.get)
    val envelope = I2b2ResultEnvelope(DefaultBreakdownResultOutputTypes.PATIENT_AGE_COUNT_XML, Map("x" -> 1, "y" -> 2))

    //BroadcastMessage
    //Non-CA-signed signing cert
    doMarshallingRoundTrip(BroadcastMessage(123456L, authn, DeleteQueryRequest(projectId, waitTime, authn, queryId), Some(Signature(XmlDateHelper.now, CertId(new BigInteger("1234567890")), None, "asdf".getBytes))))
    
    //CA-signed signing cert
    doMarshallingRoundTrip(BroadcastMessage(123456L, authn, DeleteQueryRequest(projectId, waitTime, authn, queryId), Some(Signature(XmlDateHelper.now, CertId(new BigInteger("1234567890")), Some(CertData("cert signed by ca".getBytes)), "asdf".getBytes))))

    //Non-i2b2able requests
    doMarshallingRoundTrip(ReadTranslatedQueryDefinitionRequest(authn, waitTime, queryDefinition))
    doMarshallingRoundTrip(ReadQueryResultRequest(projectId, waitTime, authn, queryId))

    //I2b2able requests
    doMarshallingRoundTrip(DeleteQueryRequest(projectId, waitTime, authn, queryId))
    doMarshallingRoundTrip(ReadApprovedQueryTopicsRequest(projectId, waitTime, authn, userId))
    doMarshallingRoundTrip(ReadInstanceResultsRequest(projectId, waitTime, authn, queryId))
    doMarshallingRoundTrip(ReadPreviousQueriesRequest(projectId, waitTime, authn, userId, fetchSize))
    doMarshallingRoundTrip(ReadQueryDefinitionRequest(projectId, waitTime, authn, queryId))
    doMarshallingRoundTrip(ReadQueryInstancesRequest(projectId, waitTime, authn, queryId))
    doMarshallingRoundTrip(RenameQueryRequest(projectId, waitTime, authn, queryId, queryName))
    doMarshallingRoundTrip(RunQueryRequest(projectId, waitTime, authn, queryId, Option(topicId), Option(topicName), outputTypes, queryDefinition))
    doMarshallingRoundTrip(RunQueryRequest(projectId, waitTime, authn, queryId, None, None, outputTypes, queryDefinition))
    doMarshallingRoundTrip(ReadResultRequest(projectId, waitTime, authn, localResultId))

    //Non-i2b2able responses
    doMarshallingRoundTrip(SingleNodeReadTranslatedQueryDefinitionResponse(SingleNodeTranslationResult(nodeId, queryDefinition)))
    doMarshallingRoundTrip(AggregatedReadTranslatedQueryDefinitionResponse(Seq(SingleNodeTranslationResult(nodeId, queryDefinition), SingleNodeTranslationResult(nodeId2, queryDefinition2))))

    //I2b2able responses
    doMarshallingRoundTrip(DeleteQueryResponse(queryId))
    doMarshallingRoundTrip(ReadApprovedQueryTopicsResponse(Seq(ApprovedTopic(queryTopicId1, queryTopicName1), ApprovedTopic(queryTopicId2, queryTopicName2))))
    doMarshallingRoundTrip(ReadInstanceResultsResponse(shrineNetworkQueryId, singleNodeResult2))
    doMarshallingRoundTrip(AggregatedReadInstanceResultsResponse(shrineNetworkQueryId, Seq(singleNodeResult1, singleNodeResult2)))
    doMarshallingRoundTrip(ReadPdoResponse(Seq(EventResponse("foo", "bar", start, end, Seq(param1))), Seq(PatientResponse("nuh", Seq(param1))), Nil))
    doMarshallingRoundTrip(ReadPreviousQueriesResponse(Seq(queryMaster1, queryMaster2)))
    doMarshallingRoundTrip(ReadQueryDefinitionResponse(42L, "name", userId, start.get, queryDefinition.toXmlString))
    doMarshallingRoundTrip(ReadQueryInstancesResponse(42L, userId, groupId, Seq(queryInstance1, queryInstance2)))
    doMarshallingRoundTrip(RenameQueryResponse(12345L, queryName))
    doMarshallingRoundTrip(RunQueryResponse(queryId, start.get, userId, groupId, queryDefinition, 12345L, singleNodeResult1))
    doMarshallingRoundTrip(AggregatedRunQueryResponse(queryId, start.get, userId, groupId, queryDefinition, 12345L, Seq(singleNodeResult1, singleNodeResult2)))
    doMarshallingRoundTrip(ReadResultResponse(42L, singleNodeResult2, envelope))
  }

  private def doMarshallingRoundTrip[T <: ShrineMessage](message: T) {
    val xml = message.toXml

    val unmarshalled = ShrineMessage.fromXml(DefaultBreakdownResultOutputTypes.toSet)(xml).get

    message match {
      //NB: Special handling of ReadInstanceResultsResponse because its member QueryRequests are munged
      //before serialization
      case readInstanceResultsResponse: ReadInstanceResultsResponse => {
        val unmarshalledResp = unmarshalled.asInstanceOf[ReadInstanceResultsResponse]

        val expected = readInstanceResultsResponse.copy(singleNodeResult = readInstanceResultsResponse.singleNodeResult.copy(instanceId = readInstanceResultsResponse.shrineNetworkQueryId))

        unmarshalledResp should equal(expected)
      }
      //NB: Special handling of AggregatedReadInstanceResultsResponse because its member QueryRequests are munged 
      //before serialization
      case aggReadInstanceResultsResponse: AggregatedReadInstanceResultsResponse => {
        val unmarshalledResp = unmarshalled.asInstanceOf[AggregatedReadInstanceResultsResponse]

        val expected = aggReadInstanceResultsResponse.copy(results = aggReadInstanceResultsResponse.results.map(_.copy(instanceId = aggReadInstanceResultsResponse.shrineNetworkQueryId)))

        unmarshalledResp.results(0).problemDigest should equal(expected.results(0).problemDigest)

        unmarshalledResp.results(0) should equal(expected.results(0))

        unmarshalledResp.results should equal(expected.results)

        unmarshalledResp should equal(expected)
      }
      //NB: Special handling of ReadQueryInstancesResponse because its member QueryInstances are not exactly preserved 
      //on serialization round trips
      case readQueryInstancesResponse: ReadQueryInstancesResponse => {
        val unmarshalledResp = unmarshalled.asInstanceOf[ReadQueryInstancesResponse]

        val expected = unmarshalledResp.withInstances(unmarshalledResp.queryInstances.map(_.copy(queryMasterId = unmarshalledResp.queryMasterId.toString)))
      }
      //NB: Special handling of RunQueryResponse because its member QueryRequest is munged 
      //during serialization
      case runQueryResponse: RunQueryResponse => {
        val unmarshalledResp = unmarshalled.asInstanceOf[RunQueryResponse]

        val expected = runQueryResponse.withResult(runQueryResponse.singleNodeResult.copy(instanceId = runQueryResponse.queryInstanceId))

        unmarshalledResp should equal(expected)
      }
      //NB: Special handling of AggregatedRunQueryResponse because its member QueryRequests are munged 
      //during serialization
      case aggRunQueryResponse: AggregatedRunQueryResponse => {
        val unmarshalledResp = unmarshalled.asInstanceOf[AggregatedRunQueryResponse]

        val expected = aggRunQueryResponse.withResults(aggRunQueryResponse.results.map(_.copy(instanceId = aggRunQueryResponse.queryInstanceId)))

        unmarshalledResp should equal(expected)
      }
      case _ => unmarshalled should equal(message)
    }
  }
}