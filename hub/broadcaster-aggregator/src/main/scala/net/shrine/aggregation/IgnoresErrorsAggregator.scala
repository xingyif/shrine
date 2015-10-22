package net.shrine.aggregation

import net.shrine.aggregation.BasicAggregator.{Invalid, Error, Valid}
import net.shrine.problem.{ProblemSources, AbstractProblem}
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.BaseShrineResponse

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
 * Extends BasicAggregator to ignore Errors and Invalid responses
 * 
 * Needs to be an abstract class instead of a trait due to the view bound on T (: Manifest)
 */
abstract class IgnoresErrorsAggregator[T <: BaseShrineResponse : Manifest] extends BasicAggregator[T] {
  private[aggregation] override def makeResponseFrom(validResponses: Iterable[Valid[T]], errorResponses: Iterable[Error], invalidResponses: Iterable[Invalid]): BaseShrineResponse = {
    //Filter out errors and invalid responses
    makeResponseFrom(validResponses)
  }

  //Default implementation, just returns first valid response, or if there are none, an ErrorResponse
  private[aggregation] def makeResponseFrom(validResponses: Iterable[Valid[T]]): BaseShrineResponse = {


    validResponses.map(_.response).toSet.headOption.getOrElse{
      val problem = NoValidResponsesToAggregate()
      ErrorResponse(problem.summary,Some(problem))
    }
  }
}

case class NoValidResponsesToAggregate() extends AbstractProblem(ProblemSources.Hub) {
  override val summary: String = "No valid responses to aggregate."

  override val description:String = "The hub received no valid responses to aggregate."
}