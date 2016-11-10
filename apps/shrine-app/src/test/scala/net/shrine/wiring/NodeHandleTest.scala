package net.shrine.wiring

import java.net.URL
import scala.concurrent.duration.DurationInt
import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import javax.ws.rs.core.MediaType
import net.shrine.adapter.client.InJvmAdapterClient
import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.broadcaster.{IdAndUrl, NodeHandle}
import net.shrine.client.JerseyHttpClient
import net.shrine.client.Poster
import net.shrine.crypto2.TrustParam.AcceptAllCerts
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.NodeId
import net.shrine.protocol.Result
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

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