package net.shrine.aggregation

import scala.concurrent.duration.DurationInt
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.I2b2ResultEnvelope
import net.shrine.protocol.NodeId
import net.shrine.protocol.QueryResult
import net.shrine.protocol.Result
import net.shrine.protocol.ResultOutputType.PATIENT_COUNT_XML
import net.shrine.protocol.RunQueryResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 *
 *
 * @author Justin Quan
 * @see http://chip.org
 * Date: 8/12/11
 */
final class RunQueryAggregatorTest extends ShouldMatchersForJUnit {
  
  private val queryId = 1234L
  private val queryName = "someQueryName"
  private val now = XmlDateHelper.now
  private val userId = "user"
  private val groupId = "group"
  private val requestQueryDef = QueryDefinition(queryName, Term("""\\i2b2\i2b2\Demographics\Age\0-9 years old\"""))
  private val requestQueryDefString = requestQueryDef.toI2b2String
  private val queryInstanceId = 9999L

  import scala.concurrent.duration._
  
  @Test
  def testAggregate {
    val qrCount = new QueryResult(1L, queryInstanceId, PATIENT_COUNT_XML, 10L, now, now, "Desc", QueryResult.StatusType.Finished)

    val rqr1 = RunQueryResponse(queryId, now, userId, groupId, requestQueryDef, queryInstanceId, qrCount)

    val result1 = Result(NodeId("description2"), 1.second, rqr1)

    val aggregator = new RunQueryAggregator(queryId, userId, groupId, requestQueryDef, true)
    
    val actual = aggregator.aggregate(Vector(result1), Nil).asInstanceOf[AggregatedRunQueryResponse]

    actual.queryId should equal(queryId)
    actual.queryInstanceId should equal(-1L)
    actual.results.size should equal(2)
    actual.results.filter(_.resultTypeIs(PATIENT_COUNT_XML)).size should equal(2) //1 for the actual count result, 1 for the aggregated total count 
    actual.results.filter(hasTotalCount).size should equal(1)
    actual.results.filter(hasTotalCount).head.setSize should equal(10)
    actual.queryName should equal(queryName)
  }

  @Test
  def testAggCount {
    val qrSet = new QueryResult(2L, queryInstanceId, PATIENT_COUNT_XML, 10L, now, now, "Desc", QueryResult.StatusType.Finished)

    val rqr1 = RunQueryResponse(queryId, now, userId, groupId, requestQueryDef, queryInstanceId, qrSet)
    val rqr2 = RunQueryResponse(queryId, now, userId, groupId, requestQueryDef, queryInstanceId, qrSet)

    val result1 = Result(NodeId("description1"), 1.second, rqr1)
    val result2 = Result(NodeId("description2"), 1.second, rqr2)

    val aggregator = new RunQueryAggregator(queryId, userId, groupId, requestQueryDef, true)
    
    //TODO: test handling error responses
    val actual = aggregator.aggregate(Vector(result1, result2), Nil).asInstanceOf[AggregatedRunQueryResponse]
    
    actual.results.filter(_.description.getOrElse("").equalsIgnoreCase("TOTAL COUNT")).head.setSize should equal(20)
  }

  @Test
  def testHandleErrorResponse {
    val qrCount = new QueryResult(1L, queryInstanceId, PATIENT_COUNT_XML, 10L, now, now, "Desc", QueryResult.StatusType.Finished)

    val rqr1 = RunQueryResponse(queryId, now, userId, groupId, requestQueryDef, queryInstanceId, qrCount)
    val errorMessage = "error message"
    val errorResponse = ErrorResponse(errorMessage)

    val result1 = Result(NodeId("description1"), 1.second, rqr1)
    val result2 = Result(NodeId("description2"), 1.second, errorResponse)

    val aggregator = new RunQueryAggregator(queryId, userId, groupId, requestQueryDef, true)
    
    val actual = aggregator.aggregate(Vector(result1, result2), Nil).asInstanceOf[AggregatedRunQueryResponse]
    
    actual.results.size should equal(3)
    
    actual.results.filter(_.resultTypeIs(PATIENT_COUNT_XML)).head.setSize should equal(10)
    actual.results.filter(_.statusType == QueryResult.StatusType.Error).head.statusMessage should equal(Some(errorMessage))
    actual.results.filter(hasTotalCount).head.setSize should equal(10)
  }

  @Test
  def testAggregateResponsesWithBreakdowns {
    def toColumnTuple(i: Int) = ("x" + i, i.toLong)
    
    val breakdowns1 = Map.empty ++ DefaultBreakdownResultOutputTypes.values.map { resultType =>
      resultType -> I2b2ResultEnvelope(resultType, (1 to 10).map(toColumnTuple).toMap)
    }
    
    val breakdowns2 = Map.empty ++ DefaultBreakdownResultOutputTypes.values.map { resultType =>
      resultType -> I2b2ResultEnvelope(resultType, (11 to 20).map(toColumnTuple).toMap)
    }
    
    val qr1 = new QueryResult(1L, queryInstanceId, Some(PATIENT_COUNT_XML), 10L, Some(now), Some(now), Some("Desc"), QueryResult.StatusType.Finished, None, breakdowns = breakdowns1)
    
    val qr2 = new QueryResult(2L, queryInstanceId, Some(PATIENT_COUNT_XML), 20L, Some(now), Some(now), Some("Desc"), QueryResult.StatusType.Finished, None, breakdowns = breakdowns2)

    val rqr1 = RunQueryResponse(queryId, now, userId, groupId, requestQueryDef, queryInstanceId, qr1)
    
    val rqr2 = RunQueryResponse(queryId, now, userId, groupId, requestQueryDef, queryInstanceId, qr2)

    val result1 = Result(NodeId("description2"), 1.second, rqr1)
    val result2 = Result(NodeId("description1"), 1.second, rqr2)

    val aggregator = new RunQueryAggregator(queryId, userId, groupId, requestQueryDef, true)
    
    val actual = aggregator.aggregate(Seq(result1, result2), Nil).asInstanceOf[AggregatedRunQueryResponse]
    
    actual.results.size should equal(3)
    actual.results.filter(hasTotalCount).size should equal(1)
    
    val Seq(actualQr1, actualQr2, actualQr3) = actual.results.filter(_.resultTypeIs(PATIENT_COUNT_XML))
    
    actualQr1.setSize should equal(10)
    actualQr2.setSize should equal(20)
    actualQr3.setSize should equal(30)
    
    actualQr1.breakdowns should equal(breakdowns1)
    actualQr2.breakdowns should equal(breakdowns2)
    
    actualQr3.breakdowns.isEmpty should be(true)
  }
  
  private def hasTotalCount(result: QueryResult) = result.description.getOrElse("").equalsIgnoreCase("TOTAL COUNT")
  
  private def toQueryResultMap(results: QueryResult*) = Map.empty ++ (for {
    result <- results
    resultType <- result.resultType
  } yield (resultType, result))
}
