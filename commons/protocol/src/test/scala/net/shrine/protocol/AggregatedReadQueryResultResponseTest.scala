package net.shrine.protocol

import junit.framework.TestCase
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 4, 2012
 */
final class AggregatedReadQueryResultResponseTest extends TestCase with ShouldMatchersForJUnit {
  import DefaultBreakdownResultOutputTypes._
  
  private val result1 = QueryResult(
    123L,
    456L,
    Some(ResultOutputType.PATIENT_COUNT_XML),
    999L,
    None,
    None,
    None,
    QueryResult.StatusType.Finished,
    None,
    Map(PATIENT_AGE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_AGE_COUNT_XML, Map("x" -> 123, "y" -> 214))))

  private val result2 = QueryResult(
    123L,
    456L,
    Some(ResultOutputType.PATIENT_COUNT_XML),
    888L,
    None,
    None,
    None,
    QueryResult.StatusType.Finished,
    None,
    Map(PATIENT_AGE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_AGE_COUNT_XML, Map("x" -> 123, "y" -> 214))))

  private val resp = AggregatedReadQueryResultResponse(123, Seq(result1, result2))

  @Test
  def testToXml {
    val expected = (<aggregatedReadQueryResultResponse><queryId>123</queryId><results>{ Seq(result1, result2).map(_.toXml) }</results></aggregatedReadQueryResultResponse>).toString

    resp.toXmlString should equal(expected)
  }

  @Test
  def testXmlRoundTrip {
    AggregatedReadQueryResultResponse.fromXml(DefaultBreakdownResultOutputTypes.toSet)(resp.toXml) should equal(resp)
  }
}