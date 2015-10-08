package net.shrine.adapter.service

import net.shrine.log.Loggable
import net.shrine.problem.{ProblemSources, AbstractProblem}
import net.shrine.protocol.NodeId
import net.shrine.protocol.Result
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.ErrorResponse
import net.shrine.adapter.AdapterMap
import net.shrine.crypto.Verifier
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import net.shrine.protocol.BaseShrineResponse

/**
 * Heart of the adapter.
 *
 * @author clint
 * @since Nov 14, 2013
 */
final class AdapterService(
  nodeId: NodeId,
  signatureVerifier: Verifier,
  maxSignatureAge: Duration,
  adapterMap: AdapterMap) extends AdapterRequestHandler with Loggable {

  import AdapterService._

  logStartup(adapterMap)

  override def handleRequest(message: BroadcastMessage): Result = {
    handleInvalidSignature(message).orElse {
      for {
        adapter <- adapterMap.adapterFor(message.request.requestType)
      } yield time(nodeId) {
        adapter.perform(message)
      }
    }.getOrElse {
      Result(nodeId, 0.milliseconds, ErrorResponse(s"Unknown request type '${message.request.requestType}'"))
    }
  }

  /**
   * @return None if the signature is fine, Some(result with an ErrorResponse) if not
   */
  private def handleInvalidSignature(message: BroadcastMessage): Option[Result] = {
    val (sigIsValid, elapsed) = time(signatureVerifier.verifySig(message, maxSignatureAge))
    
    if(sigIsValid) { None }
    else {
      info(s"Incoming message had invalid signature: $message")

      Some(Result(nodeId, elapsed.milliseconds, ErrorResponse(CouldNotVerifySignature(message))))
    }
  }
}

object AdapterService extends Loggable {
  private def logStartup(adapterMap: AdapterMap) {
    info("Adapter service initialized, will respond to the following queries: ")

    val sortedByReqType = adapterMap.requestsToAdapters.toSeq.sortBy { case (k, _) => k }

    sortedByReqType.foreach {
      case (requestType, adapter) =>
        info(s"  $requestType:\t(${adapter.getClass.getSimpleName})")
    }
  }

  private[service] def time[T](f: => T): (T, Long) = {
    val start = System.currentTimeMillis

    val result = f
    
    val elapsed = System.currentTimeMillis - start
    
    (result, elapsed)
  }
  
  private[service] def time(nodeId: NodeId)(f: => BaseShrineResponse): Result = {
    val (response, elapsed) = time(f)

    Result(nodeId, elapsed.milliseconds, response)
  }
}

case class CouldNotVerifySignature(message: BroadcastMessage) extends AbstractProblem(ProblemSources.Adapter){
  override def summary: String = s"Incoming message had invalid signature."
  override def details: String = s"Signature:\n${message.signature}"
}