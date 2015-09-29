package net.shrine.protocol

import scala.xml.NodeSeq
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlUtil
import scala.util.Success

/**
 * @author clint
 * @since Nov 5, 2012
 */
//noinspection UnitMethodIsParameterless,NameBooleanParameters,ScalaUnnecessaryParentheses
final class ShrineResponseTest extends ShouldMatchersForJUnit {
  @Test
  def testFromXml {
    //ShrineResponse.fromXml(null: String).isFailure should be(true)
    ShrineResponse.fromXml(DefaultBreakdownResultOutputTypes.toSet)(null: NodeSeq).isFailure should be(true)
    ShrineResponse.fromXml(DefaultBreakdownResultOutputTypes.toSet)(NodeSeq.Empty).isFailure should be(true)

    def roundTrip(response: ShrineResponse): Unit = {
      val unmarshalled = ShrineResponse.fromXml(DefaultBreakdownResultOutputTypes.toSet)(response.toXml)

      unmarshalled.get.getClass should equal(response.getClass)
      unmarshalled should not be (null)
      unmarshalled should equal(Success(response))
    }

    val queryResult1 = QueryResult(1L, 2342L, Some(ResultOutputType.PATIENT_COUNT_XML), 123L, None, None, None, QueryResult.StatusType.Finished, None, Map.empty)

    roundTrip(ReadQueryResultResponse(123L, queryResult1))
    roundTrip(AggregatedReadQueryResultResponse(123L, Seq(queryResult1)))
    roundTrip(DeleteQueryResponse(123L))
    roundTrip(ReadInstanceResultsResponse(2342L, queryResult1))
    roundTrip(AggregatedReadInstanceResultsResponse(2342L, Seq(queryResult1)))
    roundTrip(ReadPreviousQueriesResponse(Seq(QueryMaster("queryMasterId", 12345L, "name", "userId", "groupId", XmlDateHelper.now, Some(false)))))
    roundTrip(ReadQueryDefinitionResponse(8457L, "name", "userId", XmlDateHelper.now, "queryDefXml"))
    roundTrip(ReadQueryInstancesResponse(12345L, "userId", "groupId", Seq.empty))
    roundTrip(RenameQueryResponse(12345L, "name"))
    roundTrip(RunQueryResponse(38957L, XmlDateHelper.now, "userId", "groupId", QueryDefinition("foo", Term("bar")), 2342L, queryResult1))
    roundTrip(AggregatedRunQueryResponse(38957L, XmlDateHelper.now, "userId", "groupId", QueryDefinition("foo", Term("bar")), 2342L, Seq(queryResult1)))
    roundTrip(UnFlagQueryResponse)
    roundTrip(FlagQueryResponse)

    roundTrip(ErrorResponse("errorMessage"))
  }

  @Test
  def testToXml {
    val response = new FooResponse

    response.toXmlString should equal("<foo></foo>")
  }

  @Test
  def testToI2b2 {
    val expected = XmlUtil.stripWhitespace(<ns4:response xmlns:ns2="http://www.i2b2.org/xsd/hive/pdo/1.1/" xmlns:ns3="http://www.i2b2.org/xsd/cell/crc/pdo/1.1/" xmlns:ns4="http://www.i2b2.org/xsd/hive/msg/1.1/" xmlns:ns5="http://www.i2b2.org/xsd/cell/crc/psm/1.1/" xmlns:ns6="http://www.i2b2.org/xsd/cell/pm/1.1/" xmlns:ns7="http://sheriff.shrine.net/" xmlns:ns8="http://www.i2b2.org/xsd/cell/crc/psm/querydefinition/1.1/" xmlns:ns9="http://www.i2b2.org/xsd/cell/crc/psm/analysisdefinition/1.1/" xmlns:ns10="http://www.i2b2.org/xsd/cell/ont/1.1/" xmlns:ns11="http://www.i2b2.org/xsd/hive/msg/result/1.1/">
                                             <message_header>
                                               <i2b2_version_compatible>1.1</i2b2_version_compatible>
                                               <hl7_version_compatible>2.4</hl7_version_compatible>
                                               <sending_application>
                                                 <application_name>SHRINE</application_name>
                                                 <application_version>1.3-compatible</application_version>
                                               </sending_application>
                                               <sending_facility>
                                                 <facility_name>SHRINE</facility_name>
                                               </sending_facility>
                                               <datetime_of_message>2011-04-08T16:21:12.251-04:00</datetime_of_message>
                                               <security/>
                                               <project_id xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                                             </message_header>
                                             <response_header>
                                               <result_status>
                                                 <status type="DONE">DONE</status>
                                               </result_status>
                                             </response_header>
                                             <message_body>
                                               <foo></foo>
                                             </message_body>
                                           </ns4:response>)

    val response = new FooResponse

    response.toI2b2String should equal(expected.toString())
  }

  private final class FooResponse extends ShrineResponse {
    protected override def i2b2MessageBody = <foo></foo>

    override def toXml = i2b2MessageBody
  }
}