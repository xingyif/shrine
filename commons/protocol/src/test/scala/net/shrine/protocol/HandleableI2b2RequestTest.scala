package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term

/**
 * @author clint
 * @date Jun 18, 2014
 */
final class HandleableI2b2RequestTest extends ShouldMatchersForJUnit {
  type Req = ShrineRequest with HandleableI2b2Request

  import HandleableI2b2Request.fromI2b2
  import scala.concurrent.duration._

  private val authn = AuthenticationInfo("some-domain", "some-user", Credential("some-password", false))
  private val waitTime = 1.minute
  private val projectId = "some-project-id"

  @Test
  def testFromI2b2ReadResultOutputTypesRequest: Unit = {
    val i2b2Xml = ReadResultOutputTypesRequestTest.i2b2Xml(CrcRequestType.GetResultOutputTypes)

    val req = fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(i2b2Xml).get
    
    req.authn should equal(ReadResultOutputTypesRequestTest.authn)
    req.projectId should equal(ReadResultOutputTypesRequestTest.projectId)
    req.waitTime should equal(ReadResultOutputTypesRequestTest.waitTime)
  }

  @Test
  def testFromI2b2: Unit = {
    def roundTrip(req: Req): Unit = {
      val xml = req.toI2b2

      val unmarshalled = fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(xml).get

      (req, unmarshalled) match {
        //NB: Special handling for RunQueryRequest, which does not preserve networkQueryIds when serializing to i2b2 format
        case (expected: RunQueryRequest, actual: RunQueryRequest) => {
          //NB: When unmarshalling from i2b2 format, networkQueryId will always be -1; other fields should be fine
          actual should equal(expected.withNetworkQueryId(-1L))
        }
        case _ => unmarshalled should equal(req)
      }
    }

    //Junk input
    fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(null).isFailure should be(true)
    fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(<foo/>).isFailure should be(true)

    val queryDef = QueryDefinition("foo", Term("foo"))

    //A request that isn't a HandleableI2b2Request
    val invalidReq = ReadTranslatedQueryDefinitionRequest(authn, waitTime, queryDef)

    fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(invalidReq.toXml).isFailure should be(true)

    val networkQueryId = 12345L
    val userId = "some-user"

    fromI2b2(DefaultBreakdownResultOutputTypes.toSet)(ReadPdoRequest(projectId, waitTime, authn, "patient-set-coll-id", <foo/>).toI2b2).isFailure should be(true)

    roundTrip(DeleteQueryRequest(projectId, waitTime, authn, networkQueryId))
    roundTrip(FlagQueryRequest(projectId, waitTime, authn, networkQueryId, None))
    roundTrip(FlagQueryRequest(projectId, waitTime, authn, networkQueryId, Some("some-message")))
    roundTrip(UnFlagQueryRequest(projectId, waitTime, authn, networkQueryId))
    roundTrip(UnFlagQueryRequest(projectId, waitTime, authn, networkQueryId))
    roundTrip(ReadApprovedQueryTopicsRequest(projectId, waitTime, authn, userId))
    roundTrip(ReadInstanceResultsRequest(projectId, waitTime, authn, networkQueryId))

    //Make minimal PDO request (kind of finnicky due to embedded XML)
    val patientSetCollId = "patient-set-coll-id"
    val optionsXml = <request><pdoheader><request_type>{ CrcRequestType.GetPDOFromInputListRequestType.i2b2RequestType }</request_type></pdoheader><input_list><patient_list><patient_set_coll_id>{ patientSetCollId }</patient_set_coll_id></patient_list></input_list></request>
    val pdoXml = ReadPdoRequest.updateCollId(optionsXml.head, patientSetCollId).toSeq

    roundTrip(ReadPdoRequest(projectId, waitTime, authn, patientSetCollId, pdoXml))
    roundTrip(ReadPreviousQueriesRequest(projectId, waitTime, authn, userId, 123))
    roundTrip(ReadQueryDefinitionRequest(projectId, waitTime, authn, networkQueryId))
    roundTrip(ReadQueryInstancesRequest(projectId, waitTime, authn, networkQueryId))
    roundTrip(RenameQueryRequest(projectId, waitTime, authn, networkQueryId, "new-name"))
    roundTrip(RunQueryRequest(projectId, waitTime /*.toMillis.milliseconds*/ , authn, networkQueryId, Some("some-topic-id"), Set(ResultOutputType.PATIENT_COUNT_XML), queryDef))
    roundTrip(RunQueryRequest(projectId, waitTime /*.toMillis.milliseconds*/ , authn, networkQueryId, None, Set(ResultOutputType.PATIENT_COUNT_XML), queryDef))
  }
}