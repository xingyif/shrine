package net.shrine.integration

import net.shrine.adapter.service.AdapterRequestHandler
import net.shrine.log.Loggable
import net.shrine.protocol.NodeId
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Result
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.DeleteQueryResponse
import scala.concurrent.duration._
import scala.util.control.NoStackTrace
import net.shrine.protocol.FlagQueryRequest
import net.shrine.protocol.UnFlagQueryRequest
import net.shrine.protocol.FlagQueryResponse
import net.shrine.protocol.UnFlagQueryResponse
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.RunQueryResponse
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.QueryResult

/**
 * @author clint
 * @date Dec 17, 2013
 */
class MockAdapterRequestHandler(val nodeId: NodeId) extends AdapterRequestHandler with Loggable {
  val elapsed = 1.second

  private[this] val lock = new AnyRef
  
  @volatile private[this] var lastMessageOption: Option[BroadcastMessage] = None

  def lastMessage: Option[BroadcastMessage] = lock.synchronized(lastMessageOption)
  
  override def handleRequest(message: BroadcastMessage): Result = {
    lock.synchronized {
      lastMessageOption = Option(message)
    }

    message.request match {
      case req: DeleteQueryRequest => Result(nodeId, elapsed, DeleteQueryResponse(req.queryId))
      case req: FlagQueryRequest => Result(nodeId, elapsed, FlagQueryResponse)
      case req: UnFlagQueryRequest => Result(nodeId, elapsed, UnFlagQueryResponse)
      case req: RunQueryRequest => Result(nodeId, elapsed, {
        val now = XmlDateHelper.now
        
        val queryResult = QueryResult(34782L, 2395723L, req.outputTypes.headOption, 123L, Some(now), Some(now), None, QueryResult.StatusType.Finished, None)
        
        RunQueryResponse(message.requestId, XmlDateHelper.now, req.authn.username, req.authn.domain, req.queryDefinition, 94587L, queryResult)
      })
      case r => {
        error(s"Unexpected request: $r")
        
        ???
      }
    }
  }
}

object MockAdapterRequestHandler extends MockAdapterRequestHandler(NodeId("MOCK"))

/**
 * @author clint
 * @date Dec 17, 2013
 */
object AlwaysThrowsAdapterRequestHandler extends AdapterRequestHandler {
  override def handleRequest(message: BroadcastMessage): Result = throw new Exception("blarg") with NoStackTrace
}

/**
 * @author clint
 * @date Dec 17, 2013
 */
final case class TimesOutAdapterRequestHandler(howLong: Duration) extends AdapterRequestHandler {
  val nodeId = NodeId("TIMESOUT")

  override def handleRequest(message: BroadcastMessage): Result = message.request match {
    case req: DeleteQueryRequest => {
      Thread.sleep(howLong.toMillis)

      Result(nodeId, 1.second, DeleteQueryResponse(req.queryId))
    }
    case _ => ???
  }
}