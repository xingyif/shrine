package net.shrine.adapter

import java.sql.SQLSyntaxErrorException

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

      case e: SQLSyntaxErrorException => problemToErrorResponse(AdapterDatabaseProblem(e))

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

case class AdapterLockout(authn:AuthenticationInfo,x:AdapterLockoutException) extends AbstractProblem(ProblemSources.Adapter) {
  override val throwable = Some(x)
  override val summary: String = s"User '${authn.domain}:${authn.username}' locked out."
  override val description:String = s"User '${authn.domain}:${authn.username}' has run too many queries that produce the same result at ${x.url} ."
}

case class CrcCouldNotBeInvoked(crcUrl:String,request:ShrineRequest,x:CrcInvocationException) extends AbstractProblem(ProblemSources.Adapter) {
  override val throwable = Some(x)
  override val summary: String = s"Error communicating with I2B2 CRC."
  override val description: String = s"Error invoking the CRC at '$crcUrl' with a ${request.getClass.getSimpleName} due to ${throwable.get}."
  override val detailsXml = <details>
                              Request is {request}
                              {throwableDetail.getOrElse("")}
                            </details>
}

case class AdapterMappingProblem(x:AdapterMappingException) extends AbstractProblem(ProblemSources.Adapter) {

  override val throwable = Some(x)
  override val summary: String = "Could not map query term(s)."
  override val description = s"The Shrine Adapter on ${stamp.host.getHostName} cannot map this query to its local terms."
  override val detailsXml = <details>
                              Query Defitiontion is {x.runQueryRequest.queryDefinition}
                              RunQueryRequest is ${x.runQueryRequest.elideAuthenticationInfo}
                              {throwableDetail.getOrElse("")}
                            </details>
}

case class AdapterDatabaseProblem(x:SQLSyntaxErrorException) extends AbstractProblem(ProblemSources.Adapter) {

  override val throwable = Some(x)
  override val summary: String = "Problem in an adapter database."
  override val description = s"The Shrine Adapter on encountered a problem with a database."
}
