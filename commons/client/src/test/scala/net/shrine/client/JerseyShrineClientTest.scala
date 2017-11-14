package net.shrine.client

import net.shrine.crypto.NewTestKeyStore
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.protocol.AggregatedReadInstanceResultsResponse
import net.shrine.protocol.AggregatedReadQueryResultResponse
import net.shrine.protocol.ApprovedTopic
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.EventResponse
import net.shrine.protocol.ObservationResponse
import net.shrine.protocol.ParamResponse
import net.shrine.protocol.PatientResponse
import net.shrine.protocol.QueryResult
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.ReadPdoResponse
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.protocol.ReadQueryDefinitionResponse
import net.shrine.protocol.ReadQueryInstancesResponse
import net.shrine.protocol.RenameQueryResponse
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.QueryMaster
import net.shrine.protocol.DefaultBreakdownResultOutputTypes
import net.shrine.protocol.version.v24.AggregatedRunQueryResponse

import scala.util.Success
import scala.util.Try

/**
 *
 * @author Clint Gilbert
 * @since Sep 19, 2011
 *
 * @see http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * A client for remote ShrineResources, implemented using Jersey
 *
 */
//noinspection UnitMethodIsParameterless,NameBooleanParameters,ScalaUnnecessaryParentheses,EmptyParenMethodAccessedAsParameterless
final class JerseyShrineClientTest extends ShouldMatchersForJUnit {
  
  private val uri = "http://example.com"
  private val projectId = "alkjdasld"
  private val authn = AuthenticationInfo("domain", "user", Credential("skdhaskdhkaf", true))
  
  def testConstructor {
    val uri = "http://example.com"
    val projectId = "alkjdasld"
    val authn = AuthenticationInfo("domain", "user", Credential("skdhaskdhkaf", true))
    
    def doTestConstructor(client: JerseyShrineClient) {
      client should not be(null)
      client.shrineUrl should equal(uri)
      client.authorization should equal(authn)
      client.projectId should equal(projectId)
    }
      
    doTestConstructor(new JerseyShrineClient(uri, projectId, authn, DefaultBreakdownResultOutputTypes.toSet, AcceptAllCerts))
    doTestConstructor(new JerseyShrineClient(uri, projectId, authn, DefaultBreakdownResultOutputTypes.toSet, NewTestKeyStore.trustParam))
    
    intercept[IllegalArgumentException] {
      new JerseyShrineClient(null, projectId, authn, DefaultBreakdownResultOutputTypes.toSet, AcceptAllCerts)
    }
    
    intercept[IllegalArgumentException] {
      new JerseyShrineClient("aslkdfjaklsf", projectId, authn, DefaultBreakdownResultOutputTypes.toSet, AcceptAllCerts)
    }
    
    intercept[IllegalArgumentException] {
      new JerseyShrineClient(uri, null, authn, DefaultBreakdownResultOutputTypes.toSet, AcceptAllCerts)
    }
    
    intercept[IllegalArgumentException] {
      new JerseyShrineClient("aslkdfjaklsf", projectId, null, DefaultBreakdownResultOutputTypes.toSet, AcceptAllCerts)
    }
  }
  
  def testPerform {
    final case class Foo(x: String) {
      def toXml = <Foo><x>{ x }</x></Foo>
    }

    import JerseyShrineClient._
    
    implicit val fooDeserializer: Deserializer[Foo] = _ => xml => Try(new Foo((xml \ "x").text))

    val value = "laskjdasjklfhkasf"

    val client = new JerseyShrineClient(uri, projectId, authn, DefaultBreakdownResultOutputTypes.toSet, NewTestKeyStore.trustParam)
    
    val unmarshalled: Foo = client.perform(true)(client.webResource, _ => Foo(value).toXml.toString)

    unmarshalled should not be (null)

    val Foo(unmarshalledValue) = unmarshalled

    unmarshalledValue should equal(value)
  }

  def testDeserializers {
    def doTestDeserializer[T <: ShrineResponse](response: T, deserialize: JerseyShrineClient.Deserializer[T]) {
      val roundTripped = deserialize(DefaultBreakdownResultOutputTypes.toSet)(response.toXml)

      roundTripped should equal(Success(response))
    }
    
    val queryResult1 = QueryResult(1L, 456L, Some(ResultOutputType.PATIENT_COUNT_XML), 123L, None, None, None, QueryResult.StatusType.Finished, None)
    val queryResult2 = QueryResult(2L, 456L, Some(ResultOutputType.PATIENT_COUNT_XML), 123L, None, None, None, QueryResult.StatusType.Finished, None)

    import XmlDateHelper.now
    
    doTestDeserializer(AggregatedRunQueryResponse(123L, now, "userId", "groupId", QueryDefinition("foo", Term("bar")), 456L, Seq(queryResult1, queryResult2)), JerseyShrineClient.Deserializer.aggregatedRunQueryResponseDeserializer)

    doTestDeserializer(ReadApprovedQueryTopicsResponse(Seq(ApprovedTopic(123L, "asjkhjkas"))), JerseyShrineClient.Deserializer.readApprovedQueryTopicsResponseDeserializer)

    doTestDeserializer(ReadPreviousQueriesResponse(Seq(QueryMaster("queryMasterId", 12345L, "name", "userId", "groupId", XmlDateHelper.now, Some(false)))), JerseyShrineClient.Deserializer.readPreviousQueriesResponseDeserializer)

    doTestDeserializer(ReadQueryInstancesResponse(999L, "userId", "groupId", Seq.empty), JerseyShrineClient.Deserializer.readQueryInstancesResponseDeserializer)

    doTestDeserializer(AggregatedReadInstanceResultsResponse(1337L, Seq(dummyQueryResult(1337L))), JerseyShrineClient.Deserializer.aggregatedReadInstanceResultsResponseDeserializer)
    
    doTestDeserializer(AggregatedReadQueryResultResponse(1337L, Seq(dummyQueryResult(1337L))), JerseyShrineClient.Deserializer.aggregatedReadQueryResultResponseDeserializer)

    doTestDeserializer(ReadPdoResponse(Seq(EventResponse("event", "patient", None, None, Seq.empty)), Seq(PatientResponse("patientId", Seq(paramResponse))), Seq(ObservationResponse(None, "eventId", None, "patientId", None, None, None, "observerCode", "startDate", None, "valueTypeCode",None,None,None,None,None,None,None, Seq(paramResponse)))), JerseyShrineClient.Deserializer.readPdoResponseDeserializer)

    doTestDeserializer(ReadQueryDefinitionResponse(87456L, "name", "userId", now, "<foo/>"), JerseyShrineClient.Deserializer.readQueryDefinitionResponseDeserializer)

    doTestDeserializer(DeleteQueryResponse(56834756L), JerseyShrineClient.Deserializer.deleteQueryResponseDeserializer)

    doTestDeserializer(RenameQueryResponse(56834756L, "some-name"), JerseyShrineClient.Deserializer.renameQueryResponseDeserializer)
  }

  import ResultOutputType._
  
  private def dummyQueryResult(enclosingInstanceId: Long) = new QueryResult(123L, enclosingInstanceId, Some(PATIENT_COUNT_XML), 789L, None, None, Some("description"), QueryResult.StatusType.Finished, Some("statusMessage"), breakdowns = Map.empty)

  private def paramResponse: ParamResponse = {
    def randomString = java.util.UUID.randomUUID.toString
    
    ParamResponse(randomString, randomString, randomString)
  }
}