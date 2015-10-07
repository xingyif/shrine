package net.shrine.adapter

import net.shrine.log.Loggable
import net.shrine.problem.{Problem, ProblemNotInCodec, LoggingProblemHandler, ProblemSources, AbstractProblem}
import net.shrine.protocol.{ShrineRequest, BroadcastMessage, ErrorResponse, ShrineResponse, BaseShrineResponse, AuthenticationInfo}
import net.shrine.serialization.XmlMarshaller
import net.shrine.util.StackTrace

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
      case e: AdapterLockoutException => {
        problemToErrorResponse(AdapterLockout(message.request.authn,e))
      }
      case e @ CrcInvocationException(invokedCrcUrl, request, cause) => {
        problemToErrorResponse(CrcCouldNotBeInvoked(invokedCrcUrl,request,e))
      }
      case e: AdapterMappingException => {
        problemToErrorResponse(AdapterMappingProblem(e))
      }
      case e: Exception => {

        val summary = if(message == null) "Unknown problem in Adapter.perform with null BroadcastMessage"
                      else s"Unexpected exception in Adapter"
        problemToErrorResponse(ProblemNotInCodec(summary,e))
      }
    }

    shrineResponse
  }

  protected[adapter] def processRequest(message: BroadcastMessage): BaseShrineResponse
  
  //NOOP, may be overridden by subclasses
  def shutdown(): Unit = ()
}

case class AdapterLockout(authn:AuthenticationInfo,x:AdapterLockoutException) extends AbstractProblem(ProblemSources.Hub) {
  override def summary: String = s"User '${authn.domain}:${authn.username}' is locked out"

  override def throwable = Some(x)
}

case class CrcCouldNotBeInvoked(crcUrl:String,request:ShrineRequest,x:CrcInvocationException) extends AbstractProblem(ProblemSources.Hub) {
  override def summary: String = s"Error invoking the CRC at '$crcUrl' with request $request ."

  override def throwable = Some(x)
}

case class AdapterMappingProblem(x:AdapterMappingException) extends AbstractProblem(ProblemSources.Hub) {

  override def summary: String = s"Error mapping query terms on ${stamp.host} for query ${x.queryDefinition}"

  override def description = s"${super.description} ${x.getMessage}"

  override def throwable = Some(x)
}

case class ExceptionInAdapter()