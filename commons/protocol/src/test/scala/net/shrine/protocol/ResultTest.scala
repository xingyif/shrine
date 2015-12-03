package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import scala.xml.NodeSeq

/**
 * @author clint
 * @date Dec 3, 2013
 */
final class ResultTest extends ShouldMatchersForJUnit {
  private val resp = DeleteQueryResponse(12345L)
    
  private val nodeId = NodeId("foo")
    
  import scala.concurrent.duration._
    
  private val elapsed = 5.seconds
  
  @Test
  def testToXml {
    val result = Result(nodeId, elapsed, resp)
    
    result.toXmlString should equal(s"<shrineResult><nodeId><name>foo</name></nodeId><elapsed><value>${elapsed.length}</value><unit>${elapsed.unit}</unit></elapsed><response><deleteQueryResponse><queryId>12345</queryId></deleteQueryResponse></response></shrineResult>")
  }
  
  @Test
  def testFromXml {
    import Result.fromXml
    
    fromXml(DefaultBreakdownResultOutputTypes.toSet)(<shrineResult><nodeId><name>foo</name></nodeId><elapsed><value>{elapsed.length}</value><unit>{elapsed.unit}</unit></elapsed><response><deleteQueryResponse><queryId>12345</queryId></deleteQueryResponse></response></shrineResult>).get should equal(Result(nodeId, elapsed, resp))
    
    fromXml(DefaultBreakdownResultOutputTypes.toSet)(Nil).isFailure should be(true)
    
    fromXml(DefaultBreakdownResultOutputTypes.toSet)(<foo/>).isFailure should be(true)
  }
  
  @Test
  def testXmlRoundTrip {
    val result = Result(nodeId, elapsed, resp)
    
    Result.fromXml(DefaultBreakdownResultOutputTypes.toSet)(result.toXml).get should equal(result)
  }
}