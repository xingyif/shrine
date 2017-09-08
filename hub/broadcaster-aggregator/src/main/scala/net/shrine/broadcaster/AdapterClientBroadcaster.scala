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
import net.shrine.protocol.{AggregatedRunQueryResponse, BaseShrineResponse, BroadcastMessage, FailureResult, QueryResult, RunQueryRequest, SingleNodeResult, Timeout}
import net.shrine.status.protocol.IncrementalQueryResult

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

    //send back json containing just enough to fill a QueryResultRow
    message.request match {
      case runQueryRequest: RunQueryRequest =>
        debug(s"RunQueryRequest's nodeId is ${runQueryRequest.nodeId}")
        runQueryRequest.nodeId.fold {
          error(s"Did not send to queue because nodeId is None")
        } { nodeId => {
            val hubWillSubmitStatuses = destinations.map { nodeHandle =>
              IncrementalQueryResult(
                runQueryRequest.networkQueryId,
                nodeHandle.nodeId.name,
                QueryResult.StatusType.HubWillSubmit.name,
                s"The hub is about to submit query ${runQueryRequest.networkQueryId} to ${nodeHandle.nodeId.name}"
              )
            }.to[Seq]
          val envelope = Envelope(IncrementalQueryResult.incrementalQueryResultsEnvelopeContentsType,IncrementalQueryResult.seqToJson(hubWillSubmitStatuses))
          sendToQep(envelope,nodeId.name,s"Status update - the hub will submit the query to the adapters for ${runQueryRequest.networkQueryId}")
        }
      }
      case _ => //don't care
    }

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
                case runQueryResponse:AggregatedRunQueryResponse =>
                  val envelope = Envelope(AggregatedRunQueryResponse.getClass.getSimpleName,runQueryResponse.toXmlString)
                  sendToQep(envelope,nodeId.name,s"Result from ${runQueryResponse.results.head.description.get}")
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

  private def sendToQep(envelope: Envelope,queueName:String,logDescription:String):Unit = {

    val s: Try[Unit] = for {
      queue <- MessageQueueService.service.createQueueIfAbsent(queueName)
      sent <- MessageQueueService.service.send(envelope.toJson, queue)
    } yield sent
    s.transform({itWorked =>
      debug(s"$logDescription sent to queue")
      Success(itWorked)
    },{throwable: Throwable =>
      throwable match
      {
        case NonFatal(x) =>ExceptionWhileSendingMessage(logDescription,queueName,x)
        case _ => //no op
      }
      Failure(throwable)
    })
  }

  private[broadcaster] def callAdapter(message: BroadcastMessage, nodeHandle: NodeHandle): Future[SingleNodeResult] = {
    val NodeHandle(nodeId, client) = nodeHandle

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

case class ExceptionWhileSendingMessage(logDescription:String,queueName:String, x:Throwable) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)

  override def summary: String = s"The Hub encountered an exception while trying to send a message to $queueName"

  override def description: String = s"The Hub encountered an exception while trying to send a message about $logDescription from $queueName : ${x.getMessage}"
}