package net.shrine.adapter

import net.shrine.log.Loggable
import net.shrine.problem.{Problem, ProblemNotYetEncoded, LoggingProblemHandler, ProblemSources, AbstractProblem}
import net.shrine.protocol.{ShrineRequest, BroadcastMessage, ErrorResponse, BaseShrineResponse, AuthenticationInfo}

/**
 * @author Bill Simons
 * @since 4/8/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
abstract class Adapter extends Loggable {
  
  final def perform(message: BroadcastMessage): BaseShrineResponse = {
    def problemToErrorResponse(problem:Problem):ErrorResponse = {
      LoggingProblemHandler.handleProblem(problem)
      ErrorResponse(problem)
    }

    val shrineResponse = try {
      processRequest(message)
    } catch {
      case e: AdapterLockoutException => problemToErrorResponse(AdapterLockout(message.request.authn,e))

      case e @ CrcInvocationException(invokedCrcUrl, request, cause) => problemToErrorResponse(CrcCouldNotBeInvoked(invokedCrcUrl,request,e))

      case e: AdapterMappingException => problemToErrorResponse(AdapterMappingProblem(e))

      //noinspection RedundantBlock
      case e: Exception => {
        val summary = if(message == null) "Unknown problem in Adapter.perform with null BroadcastMessage"
                      else s"Unexpected exception in Adapter"
        problemToErrorResponse(ProblemNotYetEncoded(summary,e))
      }
    }

    shrineResponse
  }

  protected[adapter] def processRequest(message: BroadcastMessage): BaseShrineResponse
  
  //NOOP, may be overridden by subclasses
  def shutdown(): Unit = ()
}

case class AdapterLockout(authn:AuthenticationInfo,x:AdapterLockoutException) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(x)
  override val summary: String = s"User '${authn.domain}:${authn.username}' is locked out"
  override val description:String = s"User '${authn.domain}:${authn.username}' has run too many queries with the same result at ${x.url}"

}

case class CrcCouldNotBeInvoked(crcUrl:String,request:ShrineRequest,x:CrcInvocationException) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(x)
  override val summary: String = s"Error invoking the CRC at '$crcUrl' due to ${throwable.get}} ."
  override val description: String = s"Error invoking the CRC at '$crcUrl' with a ${request.getClass.getSimpleName} ."
  override val details:String =
    s"""
       |${super.details}
     """.stripMargin
}

case class AdapterMappingProblem(x:AdapterMappingException) extends AbstractProblem(ProblemSources.Hub) {

  override val throwable = Some(x)
  override val summary: String = s"Error mapping query terms on ${stamp.host}"
  override val description = s"The Shrine Adapter on ${stamp.host} cannot map this query to its local terms. Running query ${x.runQueryRequest.queryDefinition} caused ${x.cause}. This error must be corrected at the queried site."
  override val details =
    s"""${stamp.pretty}
       |Query Defitiontion is ${x.runQueryRequest.queryDefinition}
       |${throwableDetail.getOrElse("")}
       |RunQueryRequest is ${x.runQueryRequest.elideAuthenticationInfo}
     """.stripMargin
}

case class ExceptionInAdapter()