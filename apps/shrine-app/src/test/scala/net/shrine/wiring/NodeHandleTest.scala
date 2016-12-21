package net.shrine.wiring

import java.net.URL

import net.shrine.broadcaster.IdAndUrl
import net.shrine.crypto.TrustParam.AcceptAllCerts
import net.shrine.protocol.{DefaultBreakdownResultOutputTypes, NodeId}
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @since Dec 11, 2013
 */
final class NodeHandleTest extends ShouldMatchersForJUnit {
  @Test
  def testMakeNodeHandles {
    val trustParam = AcceptAllCerts

    val myId = NodeId("me")
    val xId = NodeId("x")
    val yId = NodeId("y")

    val xUrl = "http://example.com/x/requests"
    val yUrl = "http://example.com/y"

    val nodes = Seq(
      IdAndUrl(xId, new URL(xUrl)),
      IdAndUrl(yId, new URL(yUrl)))

    import scala.concurrent.duration._

    val timeout = 1.minute

    val breakdownTypes = DefaultBreakdownResultOutputTypes.toSet

  }
}