package net.shrine.adapter

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
 * @date Mar 31, 2014
 */
final class AdapterTest extends ShouldMatchersForJUnit {
  private final class MockAdapter(toReturn: => BaseShrineResponse) extends Adapter {
    override protected[adapter] def processRequest(message: BroadcastMessage): BaseShrineResponse = toReturn
  }
  
  import scala.concurrent.duration._
  
  private val lockedOutAuthn = AuthenticationInfo("d", "u", Credential("p", false))
  
  private val networkAuthn = AuthenticationInfo("nd", "nu", Credential("np", false))
  
  private val req = DeleteQueryRequest("pid", 1.second, lockedOutAuthn, 12345L)
  
  private val resp = DeleteQueryResponse(12345)
  
  @Test
  def testHandlesNonFailureCase: Unit = {
    val adapter = new MockAdapter(resp)
    
    adapter.perform(null) should equal(resp)
  }
  
  @Test
  def testHandlesLockoutCase: Unit = {
    doExceptionToErrorResponseTest(new AdapterLockoutException(lockedOutAuthn))
  }
  
  @Test
  def testHandlesCrcFailureCase: Unit = {
    val url = "http://example.com"
    
    doExceptionToErrorResponseTest(CrcInvocationException(url, req, new Exception))
  }
  
  @Test
  def testHandlesMappingFailureCase: Unit = {
    doExceptionToErrorResponseTest(new AdapterMappingException("blarg", new Exception))
  }
  
  @Test
  def testHandlesGeneralFailureCase: Unit = {
    doExceptionToErrorResponseTest(new Exception("blerg"))
  }
  
  private def doExceptionToErrorResponseTest(exception: Throwable): Unit = {
    val adapter = new MockAdapter(throw exception)
    
    adapter.perform(BroadcastMessage(networkAuthn, req)) should equal(ErrorResponse(exception.getMessage))
  } 
}