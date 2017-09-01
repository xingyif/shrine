package net.shrine.broadcaster

import net.shrine.adapter.client.{AdapterClient, RemoteAdapterClient}
import net.shrine.aggregation.RunQueryAggregator
import net.shrine.broadcaster.dao.HubDao
import net.shrine.client.TimeoutException
import net.shrine.log.Loggable
import net.shrine.messagequeueservice.MessageQueueService
import net.shrine.protocol.{AggregatedRunQueryResponse, BaseShrineResponse, BroadcastMessage, FailureResult, RunQueryRequest, RunQueryResponse, SingleNodeResult, Timeout}

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
//todo more status for SHRINE-2120
      shrineResponse: SingleNodeResult <- callAdapter(message, nodeHandle)
    } {
      try {
        message.request match {
          case runQueryRequest:RunQueryRequest =>
            debug(s"RunQueryRequest's nodeId is ${runQueryRequest.nodeId}")
            //todo SHRINE-2120 send to the QEP queue named nodeId
            //todo get to the point where there's always a nodeId and clean this up
            runQueryRequest.nodeId.fold{
              debug(s"Did not send to queue because nodeId is None")
            }{ nodeId =>

              //todo make a RunQueryResponse from the SingleNodeResult
              val aggregator = new RunQueryAggregator( //to convert the SingleNodeResult into a RunQueryResponse
                runQueryRequest.networkQueryId,
                runQueryRequest.authn.username,
                runQueryRequest.authn.domain,
                runQueryRequest.queryDefinition,
                false
              )

              val response: BaseShrineResponse = aggregator.aggregate(Seq(shrineResponse),Seq.empty,message)

              response match {
                case runQueryResponse:AggregatedRunQueryResponse => sendToQep(runQueryResponse,nodeId.name)
                case _ => debug(s"response is not a RunQueryResponse. It is ${response.toString}") //todo really good error message
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
    val s: Try[Unit] = for {
      queue <- MessageQueueService.service.createQueueIfAbsent(queueName)
      //todo use the json envelope when you get to SHRINE-2177
      sent <- MessageQueueService.service.send(runQueryResponse.toXmlString, queue)
    } yield sent
    s.transform({itWorked =>
      debug(s"Result from ${runQueryResponse.results.head.description.get} sent to queue")
      Success(itWorked)
    },{throwable: Throwable =>
      throwable match
      {
        case NonFatal(x) => error(s"Could not send result from hub to $queueName for ${runQueryResponse.queryId}", x) //todo better error handling
        case _ => //no op
      }
      Failure(throwable)
    })
  }

  private[broadcaster] def callAdapter(message: BroadcastMessage, nodeHandle: NodeHandle): Future[SingleNodeResult] = {
    val NodeHandle(nodeId, client) = nodeHandle

    client.query(message).recover {
      case e: TimeoutException =>
        error(s"Broadcasting to $nodeId timed out")
        Timeout(nodeId)

      case NonFatal(e) =>
        error(s"Broadcasting to $nodeId failed with ", e)
        FailureResult(nodeId, e)

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