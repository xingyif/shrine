package net.shrine.protocol

import junit.framework.TestCase
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Nov 2, 2012
 */
final class ReadQueryResultResponseTest extends TestCase with ShouldMatchersForJUnit {
  import DefaultBreakdownResultOutputTypes._
  
  private val result = QueryResult(
    resultId = 123L,
    instanceId = 456L,
    resultType = Some(ResultOutputType.PATIENT_COUNT_XML),
    setSize = 999L,
    startDate = None,
    endDate = None,
    description = None,
    statusType = QueryResult.StatusType.Finished,
    statusMessage = None,
    breakdowns = Map(PATIENT_AGE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_AGE_COUNT_XML, Map("x" -> 123, "y" -> 214))))

  private val resp = ReadQueryResultResponse(123, result)

  @Test
  def testToXml {
    val expected = (<readQueryResultResponse><queryId>123</queryId><results>{ result.toXml }</results></readQueryResultResponse>).toString

    resp.toXmlString should equal(expected)
  }

  @Test
  def testXmlRoundTrip {
    import DefaultBreakdownResultOutputTypes.{ values => breakdownTypes }
    
    ReadQueryResultResponse.fromXml(breakdownTypes.toSet)(resp.toXml) should equal(resp)
  }
}