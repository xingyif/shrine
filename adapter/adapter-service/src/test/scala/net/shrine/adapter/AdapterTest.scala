package net.shrine.adapter

import net.shrine.problem.ProblemNotInCodec
import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.BaseShrineResponse
import net.shrine.protocol.BroadcastMessage
import org.junit.Test
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.DeleteQueryRequest

/**
 * @author clint
 * @since Mar 31, 2014
 */
//noinspection UnitMethodIsParameterless
final class AdapterTest extends ShouldMatchersForJUnit {
  private final class MockAdapter(toReturn: => BaseShrineResponse) extends Adapter {
    override protected[adapter] def processRequest(message: BroadcastMessage): BaseShrineResponse = toReturn
  }
  
  import scala.concurrent.duration._
  
  private val lockedOutAuthn = AuthenticationInfo("d", "u", Credential("p", isToken = false))
  
  private val networkAuthn = AuthenticationInfo("nd", "nu", Credential("np", isToken = false))
  
  private val req = DeleteQueryRequest("pid", 1.second, lockedOutAuthn, 12345L)
  
  private val resp = DeleteQueryResponse(12345)

  @Test
  def testHandlesNonFailureCase: Unit = {
    val adapter = new MockAdapter(resp)
    
    adapter.perform(null) should equal(resp)
  }

  @Test
  def testHandlesLockoutCase: Unit = {
    doErrorResponseTest(new AdapterLockoutException(lockedOutAuthn),classOf[AdapterLockout])
  }

  @Test
  def testHandlesCrcFailureCase: Unit = {
    val url = "http://example.com"
    
    doErrorResponseTest(CrcInvocationException(url, req, new Exception),classOf[CrcCouldNotBeInvoked])
  }
  
  @Test
  def testHandlesMappingFailureCase: Unit = {
    doErrorResponseTest(new AdapterMappingException(QueryDefinition("test query",None),"blarg", new Exception),classOf[AdapterMappingProblem])
  }
  
  @Test
  def testHandlesGeneralFailureCase: Unit = {
    doErrorResponseTest(new Exception("blerg"),classOf[ProblemNotInCodec])
  }

  //noinspection ScalaUnreachableCode,RedundantBlock
  private def doErrorResponseTest(exception: Throwable,problemClass:Class[_]) = {
    val adapter = new MockAdapter(throw exception)

    val response = adapter.perform(BroadcastMessage(networkAuthn, req))

    response match {
      case errorResponse:ErrorResponse => {
        val pd = errorResponse.problemDigest
        pd.codec should be (problemClass.getName)
      }
      case x => fail(s"$x is not an ErrorResponse")
    }
  }
}