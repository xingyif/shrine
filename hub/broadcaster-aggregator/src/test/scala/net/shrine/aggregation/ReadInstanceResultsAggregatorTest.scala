package net.shrine.aggregation

import net.shrine.problem.ProblemNotYetEncoded
import org.junit.Test
import org.junit.Assert.assertNotNull
import net.shrine.protocol.{ ErrorResponse, QueryResult, ReadInstanceResultsResponse }
import net.shrine.protocol.ResultOutputType._
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.AggregatedReadInstanceResultsResponse
import net.shrine.protocol.Result
import net.shrine.protocol.NodeId
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author Bill Simons
 * @since 6/13/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
//noinspection UnitMethodIsParameterless,NameBooleanParameters
final class ReadInstanceResultsAggregatorTest extends ShouldMatchersForJUnit {

  import scala.concurrent.duration._
  
  @Test
  def testAggregate {
    val instanceId = 123L
    val startDate = XmlDateHelper.now
    val endDate = XmlDateHelper.now
    
    val queryResult1 = new QueryResult(1L, instanceId, PATIENT_COUNT_XML, 12, startDate, endDate, QueryResult.StatusType.Finished)
    val queryResult2 = new QueryResult(2L, instanceId, PATIENT_COUNT_XML, 14, startDate, endDate, QueryResult.StatusType.Finished)

    val aggregator = new ReadInstanceResultsAggregator(instanceId, true)
    val aggregatorNoAggregate = new ReadInstanceResultsAggregator(instanceId, false)

    val response1 = ReadInstanceResultsResponse(instanceId, queryResult1)
    val response2 = ReadInstanceResultsResponse(instanceId, queryResult2)

    val description1 = "NODE1"
    val description2 = "NODE2"

    val result1 = Result(NodeId(description1), 1.second, response1)
    val result2 = Result(NodeId(description2), 5.seconds, response2)

    {
      val actual = aggregator.aggregate(Seq(result1, result2), Nil).asInstanceOf[AggregatedReadInstanceResultsResponse]

      assertNotNull(actual)
      assertNotNull(actual.results)
      
      actual.results.size should equal(3)
      
      val expectedTotal = queryResult1.setSize + queryResult2.setSize
      
      actual.results.contains(queryResult1.withDescription(description1)) should be(true)
      actual.results.contains(queryResult2.withDescription(description2).withResultType(PATIENT_COUNT_XML)) should be(true)
      actual.results.exists(qr => qr.setSize == expectedTotal && qr.resultTypeIs(PATIENT_COUNT_XML) && qr.description.contains("Aggregated Count")) should be(true)
    }

    {
      val actual = aggregatorNoAggregate.aggregate(Seq(result1, result2), Nil).asInstanceOf[AggregatedReadInstanceResultsResponse]

      assertNotNull(actual)
      assertNotNull(actual.results)
      
      actual.results.size should equal(2)
      
      actual.results.contains(queryResult1.withDescription(description1)) should be(true)
      actual.results.contains(queryResult2.withDescription(description2).withResultType(PATIENT_COUNT_XML)) should be(true)
    }
  }

  @Test
  def testAggregateWithError {
    val instanceId = 123L
    val startDate = XmlDateHelper.now
    val endDate = XmlDateHelper.now
    val queryResult = new QueryResult(1L, instanceId, PATIENT_COUNT_XML, 12, startDate, endDate, QueryResult.StatusType.Finished)
    val aggregator = new ReadInstanceResultsAggregator(instanceId, true)
    val errorMessage = "you are an error"

    val patientCountResponse = ReadInstanceResultsResponse(instanceId, queryResult)
    val errorResponse = ErrorResponse(errorMessage)

    val patientCountNodeDescription = "NODE1"
    val errorNodeDescription = "NODE2"
      
    val result1 = Result(NodeId(patientCountNodeDescription), 1.minute, patientCountResponse)
    val result2 = Result(NodeId(errorNodeDescription), 1.hour, errorResponse)

    val actual = aggregator.aggregate(Seq(result1, result2), Nil).asInstanceOf[AggregatedReadInstanceResultsResponse]

    assertNotNull(actual)
    assertNotNull(actual.results)
    
    actual.results.size should equal(3)
    
    actual.results.contains(queryResult.withDescription(patientCountNodeDescription)) should be(true)

    actual.results.exists(qr => qr.problemDigest.exists(pd => pd.codec == classOf[ProblemNotYetEncoded].getName)) should be (true)
  }
}