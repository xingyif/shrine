package net.shrine.util

import com.sun.jersey.test.framework.JerseyTest
import scala.util.Try
import java.net.Socket
import java.io.IOException

/**
 * @author clint
 * @date Oct 23, 2014
 */
abstract class AbstractPortSearchingJerseyTest extends JerseyTest {
  
  //Attempt to find the next available port, starting from defaultPort.  This creates a race condition
  //in that other JerseyTests could "steal" the port returned by this method by binding to the port after
  //this method returns and before the result is used to start an embedded HTTP server, but this is
  //better than always failing in the face of a parallelized build, or another concurrent build on the
  //same machine, as was previously the case.
  override protected def getPort(defaultPort: Int): Int = {
    Try(nextAvailablePort(defaultPort)).getOrElse(super.getPort(defaultPort))
  }

  private def nextAvailablePort(start: Int): Int = {
    Iterator.from(start).take(100).takeWhile(isAvailable).next
  }
  
  private def isAvailable(port: Int): Boolean = {
    var s: Socket = null

    try {
      s = new Socket("localhost", port)

      false
    } catch {
      case e: IOException => true
    } finally {
      if (s != null) {
        s.close()
      }
    }
  }
}