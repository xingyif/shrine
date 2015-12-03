package net.shrine.protocol

import scala.xml.NodeSeq
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import scala.xml.XML

/**
 * @author clint
 * @since Mar 22, 2013
 */
final class ShrineRequestTest extends ShouldMatchersForJUnit {
  @Test
  def testFromXmlThrowsOnBadInput {
    intercept[Exception] {
      ShrineRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(XML.loadString("asdasdasddas"))
    }
  }
  
  @Test
  def testFromXml {
    val projectId = "salkdjksaljdkla"
      
    import scala.concurrent.duration._
      
    val waitTime: Duration = 98374L.milliseconds
    val userId = "foo-user"
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
    val localResultId = "aoiduaojsdpaojcmsal"
    
    def doMarshallingRoundTrip(req: ShrineRequest) {
      val xml = req.toXml
      
      val unmarshalled = ShrineRequest.fromXml(DefaultBreakdownResultOutputTypes.toSet)(xml)
      
      req match {
        //NB: Special handling of ReadPdoRequest due to fiddly serialization and equality issues with its NodeSeq field. :( :(
        case readPdoRequest: ReadPdoRequest => {
          val unmarshalledReadPdoRequest = unmarshalled.get.asInstanceOf[ReadPdoRequest]
          
          readPdoRequest.projectId should equal(unmarshalledReadPdoRequest.projectId)
          readPdoRequest.waitTime should equal(unmarshalledReadPdoRequest.waitTime)
          readPdoRequest.authn should equal(unmarshalledReadPdoRequest.authn)
          readPdoRequest.patientSetCollId should equal(unmarshalledReadPdoRequest.patientSetCollId)
          //NB: Ugh :(
          readPdoRequest.optionsXml.toString should equal(unmarshalledReadPdoRequest.optionsXml.toString)
        }
        case _ => unmarshalled.get should equal(req)
      }
    }
    
    //doMarshallingRoundTrip(ReadQueryResultRequest(projectId, waitTime, authn, queryId))
    doMarshallingRoundTrip(DeleteQueryRequest(projectId, waitTime, authn, queryId))
    doMarshallingRoundTrip(ReadApprovedQueryTopicsRequest(projectId, waitTime, authn, userId))
    doMarshallingRoundTrip(ReadInstanceResultsRequest(projectId, waitTime, authn, queryId))
    doMarshallingRoundTrip(ReadPdoRequest(projectId, waitTime, authn, patientSetCollId, optionsXml))
    doMarshallingRoundTrip(ReadPreviousQueriesRequest(projectId, waitTime, authn, userId, fetchSize))
    doMarshallingRoundTrip(ReadQueryDefinitionRequest(projectId, waitTime, authn, queryId))
    doMarshallingRoundTrip(ReadQueryInstancesRequest(projectId, waitTime, authn, queryId))
    doMarshallingRoundTrip(RenameQueryRequest(projectId, waitTime, authn, queryId, queryName))
    doMarshallingRoundTrip(RunQueryRequest(projectId, waitTime, authn, queryId, Option(topicId), Option(topicName), outputTypes, queryDefinition))
    doMarshallingRoundTrip(RunQueryRequest(projectId, waitTime, authn, queryId, None, None, outputTypes, queryDefinition))
    doMarshallingRoundTrip(ReadResultRequest(projectId, waitTime, authn, localResultId))
    doMarshallingRoundTrip(FlagQueryRequest(projectId, waitTime, authn, queryId, None))
    doMarshallingRoundTrip(FlagQueryRequest(projectId, waitTime, authn, queryId, Some("some-message")))
    doMarshallingRoundTrip(UnFlagQueryRequest(projectId, waitTime, authn, queryId))
  }
}