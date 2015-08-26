package net.shrine.broadcaster

import net.shrine.log.Loggable

import scala.concurrent.Future
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.SingleNodeResult
import java.net.SocketTimeoutException
import net.shrine.protocol.Timeout
import net.shrine.protocol.Failure
import scala.util.control.NonFatal
import net.shrine.protocol.NodeId
import net.shrine.client.TimeoutException
import net.shrine.adapter.client.RemoteAdapterClient
import net.shrine.adapter.client.AdapterClient
import net.shrine.broadcaster.dao.HubDao
import net.shrine.protocol.RunQueryRequest

/**
 * @author clint
 * @date Nov 15, 2013
 */
final case class AdapterClientBroadcaster(destinations: Set[NodeHandle], dao: HubDao) extends Broadcaster with Loggable {

  logStartup()

  import scala.concurrent.ExecutionContext.Implicits.global

  override def broadcast(message: BroadcastMessage): Multiplexer = {
    logOutboundIfNecessary(message)

    val multiplexer: Multiplexer = new BufferingMultiplexer(destinations.map(_.nodeId))

    for {
      nodeHandle <- destinations
      shrineResponse <- callAdapter(message, nodeHandle)
    } {
      try { multiplexer.processResponse(shrineResponse) }
      finally { logResultsIfNecessary(message, shrineResponse) }
    }

    multiplexer
  }

  private[broadcaster] def callAdapter(message: BroadcastMessage, nodeHandle: NodeHandle): Future[SingleNodeResult] = {
    val NodeHandle(nodeId, client) = nodeHandle

    client.query(message).recover {
      case e: TimeoutException => {
        error(s"Broadcasting to $nodeId timed out")

        Timeout(nodeId)
      }
      case NonFatal(e) => {
        error(s"Broadcasting to $nodeId failed with ", e)

        Failure(nodeId, e)
      }
    }
  }

  private[broadcaster] def logResultsIfNecessary(message: BroadcastMessage, result: SingleNodeResult): Unit = logIfNecessary(message) { _ =>
    dao.logQueryResult(message.requestId, result)
  }

  private[broadcaster] def logOutboundIfNecessary(message: BroadcastMessage): Unit = logIfNecessary(message) { runQueryReq =>
    dao.logOutboundQuery(message.requestId, message.networkAuthn, runQueryReq.queryDefinition)
  }

  private[broadcaster] def logIfNecessary(message: BroadcastMessage)(f: RunQueryRequest => Any): Unit = {
    message.request match {
      case runQueryReq: RunQueryRequest => f(runQueryReq)
      case _ => ()
    }
  }

  private def logStartup(): Unit = {
    def clientToString(client: AdapterClient): String = client match {
      case r: RemoteAdapterClient => r.poster.url.toString
      case _ => "<in-JVM>"
    }

    info(s"Initialized ${getClass.getSimpleName}, will broadcast to the following destinations:")

    destinations.toSeq.sortBy(_.nodeId.name).foreach { handle => 
      info(s"  ${handle.nodeId}: ${clientToString(handle.client)}")
    }
  }
}