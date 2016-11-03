package net.shrine.adapter.service

import net.shrine.log.Loggable
import net.shrine.protocol.{BaseShrineResponse, BroadcastMessage, ErrorResponse, NodeId, RequestType, Result, Signature}
import net.shrine.adapter.AdapterMap
import net.shrine.crypto.Verifier
import net.shrine.problem.{AbstractProblem, ProblemSources}

import scala.concurrent.duration.Duration
import scala.concurrent.duration._

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
      Result(nodeId, 0.milliseconds, ErrorResponse(UnknownRequestType(message.request.requestType)))
    }
  }

  /**
   * @return None if the signature is fine, Some(result with an ErrorResponse) if not
   */
  private def handleInvalidSignature(message: BroadcastMessage): Option[Result] = {
    val (sigIsValid, elapsed) = time(signatureVerifier.verifySig(message, maxSignatureAge))
    println(s"HEY! $sigIsValid")
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

  val signature: Option[Signature] = message.signature

  override val summary: String = signature.fold("A message was not signed")(sig => s"The trust relationship with ${sig.signedBy} is not properly configured.")
  override val description: String = signature.fold(s"The Adapter at ${stamp.host.getHostName} could not properly validate a request because it had no signature.")(sig => s"The Adapter at ${stamp.host.getHostName} could not properly validate the request from ${sig.signedBy}. An incoming message from the hub had an invalid signature.")
  override val detailsXml = signature.fold(
    <details/>
  )(
      sig =>  <details>
                Signature is {sig}
              </details>
    )
}

case class UnknownRequestType(requestType: RequestType) extends AbstractProblem(ProblemSources.Adapter){

  override val summary: String = s"Unknown request type $requestType"
  override val description: String = s"The Adapter at ${stamp.host.getHostName} received a request of type $requestType that it cannot process."
}