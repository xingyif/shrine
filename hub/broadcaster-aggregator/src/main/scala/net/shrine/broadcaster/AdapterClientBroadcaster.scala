package net.shrine.broadcaster

import net.shrine.adapter.client.{AdapterClient, RemoteAdapterClient}
import net.shrine.aggregation.RunQueryAggregator
import net.shrine.audit.NetworkQueryId
import net.shrine.broadcaster.dao.HubDao
import net.shrine.client.TimeoutException
import net.shrine.log.Loggable
import net.shrine.messagequeueservice.MessageQueueService
import net.shrine.messagequeueservice.protocol.Envelope
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.{AggregatedRunQueryResponse, BaseShrineResponse, BroadcastMessage, FailureResult, RunQueryRequest, SingleNodeResult, Timeout}

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

/**
 * @author clint
 * @since Nov 15, 2013
 */
final case class AdapterClientBroadcaster(destinations: Set[NodeHandle], dao: HubDao) extends Broadcaster with Loggable {

  logStartup()

  import scala.concurrent.ExecutionContext.Implicits.global

  override def broadcast(message: BroadcastMessage): Multiplexer = {
    logOutboundIfNecessary(message)

    val multiplexer: Multiplexer = new BufferingMultiplexer(destinations.map(_.nodeId))

    for {
      nodeHandle <- destinations
      shrineResponse: SingleNodeResult <- callAdapter(message, nodeHandle)
    } {
      try {
        message.request match {
          case runQueryRequest:RunQueryRequest =>
            debug(s"RunQueryRequest's nodeId is ${runQueryRequest.nodeId}")
            runQueryRequest.nodeId.fold{
              error(s"Did not send to queue because nodeId is None")
            }{ nodeId =>

              // make an AggregateRunQueryResponse from the SingleNodeResult
              val aggregator = new RunQueryAggregator( //to convert the SingleNodeResult into an AggregateRunQueryResponse
                runQueryRequest.networkQueryId,
                runQueryRequest.authn.username,
                runQueryRequest.authn.domain,
                runQueryRequest.queryDefinition,
                false
              )

              val response: BaseShrineResponse = aggregator.aggregate(Seq(shrineResponse),Seq.empty,message)

              response match {
                case runQueryResponse:AggregatedRunQueryResponse => sendToQep(runQueryResponse,nodeId.name)
                case _ => error(s"response is not a AggregatedRunQueryResponse. It is ${response.toString}")
              }
            }
          case _ => debug(s"Not a RunQueryRequest but a ${message.request.getClass.getSimpleName}.")
        }

        multiplexer.processResponse(shrineResponse) }
      finally { logResultsIfNecessary(message, shrineResponse) }
    }

    multiplexer
  }

  private def sendToQep(runQueryResponse: AggregatedRunQueryResponse,queueName:String):Unit = {
    val envelope = Envelope(AggregatedRunQueryResponse.getClass.getSimpleName,runQueryResponse.toXmlString)

    val s: Try[Unit] = for {
      queue <- MessageQueueService.service.createQueueIfAbsent(queueName)
      sent <- MessageQueueService.service.send(envelope.toJson, queue)
    } yield sent
    s.transform({itWorked =>
      debug(s"Result from ${runQueryResponse.results.head.description.get} sent to queue")
      Success(itWorked)
    },{throwable: Throwable =>
      throwable match
      {
        case NonFatal(x) =>ExceptionWhileSendingMessage(runQueryResponse.queryId,queueName,x)
        case _ => //no op
      }
      Failure(throwable)
    })
  }

  private[broadcaster] def callAdapter(message: BroadcastMessage, nodeHandle: NodeHandle): Future[SingleNodeResult] = {
    val NodeHandle(nodeId, client) = nodeHandle

    //todo more status for SHRINE-2122
    // todo send back json containing just enough to fill a QueryResultRow
    // may need to do SHRINE-2177 to make this work
    client.query(message).recover {
      case e: TimeoutException =>
        error(s"Broadcasting to $nodeId timed out")
        Timeout(nodeId)

      case NonFatal(e) =>
        error(s"Broadcasting to $nodeId failed with ", e)
        FailureResult(nodeId, e)

    }
    //todo more status for SHRINE-2123
    // todo send back json containing just enough to fill a QueryResultRow
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

case class ExceptionWhileSendingMessage(networkQueryId: NetworkQueryId,queueName:String, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)

  override def summary: String = s"The Hub encountered an exception while trying to send a message to $queueName"

  override def description: String = s"The Hub encountered an exception while trying to send a message about $networkQueryId from $queueName : ${x.getMessage}"
}