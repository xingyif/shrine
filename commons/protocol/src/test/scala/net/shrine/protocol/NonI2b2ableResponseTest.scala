package net.shrine.protocol

import junit.framework.TestCase
import net.shrine.problem.TurnOffProblemConnector
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Apr 30, 2013
 */
final class NonI2b2ableResponseTest extends TestCase with ShouldMatchersForJUnit with TurnOffProblemConnector {
  object TestResponse extends ShrineResponse with NonI2b2ableResponse {
    def messageBody = i2b2MessageBody
    
    override def toXml = ???
  }
  
  @Test
  def testI2b2MessageBody {
    intercept[Error] {
      TestResponse.messageBody
    }
  }
  
  @Test
  def testToI2b2 {
    val testResponseClassName = TestResponse.getClass.getSimpleName
    
    ErrorResponse.fromI2b2(TestResponse.toI2b2).errorMessage.contains(testResponseClassName) should be(true)
  }
}