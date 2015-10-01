package net.shrine.aggregation

import net.shrine.problem.{ProblemSources, AbstractProblem}
import net.shrine.protocol.ShrineResponse
import net.shrine.aggregation.BasicAggregator.{Invalid, Error, Valid}
import net.shrine.protocol.QueryResult

/**
 *
 * @author Clint Gilbert
 * @since Sep 16, 2011
 *
 * @see http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * Extends BasicAggregator to package Errors and Invalid responses into QueryResults
 * 
 * Needs to be an abstract class instead of a trait due to the view bound on T (: Manifest)
 */
abstract class PackagesErrorsAggregator[T <: ShrineResponse : Manifest](
    errorMessage: Option[String] = None, 
    invalidMessage: Option[String] = None) extends BasicAggregator[T] {
  
  private[aggregation] def makeErrorResult(error: Error): QueryResult = { 
    val Error(originOption, errorResponse) = error

    //Use node name as the description, to avoid giving the web UI more data than it can display
    val desc = originOption.map(_.name) 
    
    QueryResult.errorResult(desc, errorMessage.getOrElse(errorResponse.errorMessage),Option(ErrorResultProblem(error)))
  }
  
  private[aggregation] def makeInvalidResult(invalid: Invalid): QueryResult = {
    val Invalid(originOption, errorMessage) = invalid 
    
    //Use node name as the description, to avoid giving the web UI more data than it can display
    val desc = originOption.map(_.name)
    
    QueryResult.errorResult(desc, invalidMessage.getOrElse(errorMessage),Option(InvalidResultProblem(invalid)))
  }
  
  private[aggregation] final override def makeResponseFrom(validResponses: Iterable[Valid[T]], errorResponses: Iterable[Error], invalidResponses: Iterable[Invalid]): ShrineResponse = {
    makeResponse(validResponses, errorResponses.map(makeErrorResult), invalidResponses.map(makeInvalidResult))
  }
  
  private[aggregation] def makeResponse(validResponses: Iterable[Valid[T]], errorResponses: Iterable[QueryResult], invalidResponses: Iterable[QueryResult]): ShrineResponse
}

//todo Problem these two should really propagate problems from the Error or the Invalid result
case class ErrorResultProblem(error:Error) extends AbstractProblem(ProblemSources.Hub) {
  override def summary: String = s"${error.response.errorMessage} on ${error.origin.getOrElse("unknown node")}"
}

case class InvalidResultProblem(invalid:Invalid) extends AbstractProblem(ProblemSources.Hub) {
  override def summary: String = s"${invalid.errorMessage} on ${invalid.origin.getOrElse("unknown node")}"
}