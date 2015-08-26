package net.shrine.adapter.client

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import scala.util.control.NoStackTrace
import com.sun.jersey.api.client.UniformInterfaceException
import java.net.SocketTimeoutException
import com.sun.jersey.api.client.ClientHandlerException

/**
 * @author clint
 * @date Dec 17, 2013
 */
final class RemoteAdapterClientTest extends ShouldMatchersForJUnit {
  @Test
  def testIsTimeout {
    import RemoteAdapterClient.isTimeout
    
    isTimeout(new Exception) should be(false)
    
    isTimeout(new IllegalArgumentException) should be(false)
    
    def jaxrsException(cause: Option[Throwable] = None): Exception = {
      val e = new ClientHandlerException("")
      
      cause.foreach(e.initCause)
      
      e
    }
    
    isTimeout(jaxrsException()) should be(false)
    
    isTimeout(jaxrsException(Some(new SocketTimeoutException))) should be(true)
    
    isTimeout(new SocketTimeoutException) should be(true)
  }
}