package net.shrine.adapter

import java.sql.SQLException

import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, LoggingProblemHandler, Problem, ProblemNotYetEncoded, ProblemSources}
import net.shrine.protocol.{AuthenticationInfo, BaseShrineResponse, BroadcastMessage, ErrorResponse, ShrineRequest}

import scala.util.control.NonFatal

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
  
  //noinspection RedundantBlock
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

      case e: SQLException => problemToErrorResponse(AdapterDatabaseProblem(e))

      case NonFatal(e) => {
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
  override lazy val throwable = Some(x)
  override lazy val summary: String = s"User '${authn.domain}:${authn.username}' locked out."
  override lazy val description:String = s"User '${authn.domain}:${authn.username}' has run too many queries that produce the same result at ${x.url} ."
}

case class CrcCouldNotBeInvoked(crcUrl:String,request:ShrineRequest,x:CrcInvocationException) extends AbstractProblem(ProblemSources.Adapter) {
  override lazy val throwable = Some(x)
  override lazy val summary: String = s"Error communicating with I2B2 CRC."
  override lazy val description: String = s"Error invoking the CRC at '$crcUrl' with a ${request.getClass.getSimpleName} due to ${throwable.get}."
  override lazy val detailsXml = <details>
                              Request is {request}
                              {throwableDetail.getOrElse("")}
                            </details>
}

case class AdapterMappingProblem(x:AdapterMappingException) extends AbstractProblem(ProblemSources.Adapter) {

  override lazy val throwable = Some(x)
  override lazy val summary: String = "Could not map query term(s)."
  override lazy val description = s"The Shrine Adapter on ${stamp.host.getHostName} cannot map this query to its local terms."
  override lazy val detailsXml = <details>
                              Query Defitiontion is {x.runQueryRequest.queryDefinition}
                              RunQueryRequest is ${x.runQueryRequest.elideAuthenticationInfo}
                              {throwableDetail.getOrElse("")}
                            </details>
}

case class AdapterDatabaseProblem(x:SQLException) extends AbstractProblem(ProblemSources.Adapter) {

  override lazy val throwable = Some(x)
  override lazy val summary: String = "Problem using the Adapter database."
  override lazy val description = "The Shrine Adapter encountered a problem using a database."
}