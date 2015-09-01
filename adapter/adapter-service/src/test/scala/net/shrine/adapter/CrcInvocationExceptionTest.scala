package net.shrine.adapter

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.RunQueryRequest

/**
 * @author clint
 * @since Oct 23, 2014
 */
final class CrcInvocationExceptionTest extends ShouldMatchersForJUnit {
  @Test
  def testApply(): Unit = {
    import scala.concurrent.duration._

    val authn = AuthenticationInfo("d", "p", Credential("alksfh", isToken = false))
    
    val rootCause = new Exception with scala.util.control.NoStackTrace
    
    val url = "http://example.com"
    
    val deleteReq = DeleteQueryRequest("project-id", 1.minute, authn, 12345L)
    
    val runQueryReq = RunQueryRequest("project-id", 1.minute, authn, 123245L, None, Set.empty, null)
    
    {
      val e = CrcInvocationException(url, deleteReq, rootCause)
    
      e.invokedUrl should equal(url)
      e.rootCause should equal(rootCause)
      e.request should equal(deleteReq)
    }
    
    {
      val e = CrcInvocationException(url, runQueryReq, rootCause)
    
      e.invokedUrl should equal(url)
      e.rootCause should equal(rootCause)
      e.request should equal(runQueryReq.elideAuthenticationInfo)
    }
  }
}