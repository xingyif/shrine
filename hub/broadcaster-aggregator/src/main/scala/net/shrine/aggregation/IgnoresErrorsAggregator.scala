package net.shrine.aggregation

import net.shrine.aggregation.BasicAggregator.{Error, Invalid, Valid}
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.{BaseShrineResponse, BroadcastMessage, ErrorResponse, RequestType}

import scala.xml.NodeSeq

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
  private[aggregation] override def makeResponseFrom(validResponses: Iterable[Valid[T]],
                                                     errorResponses: Iterable[Error],
                                                     invalidResponses: Iterable[Invalid],
                                                     respondingTo: BroadcastMessage): BaseShrineResponse = {
    //Filter out errors and invalid responses
    makeResponseFrom(validResponses,respondingTo)
  }

  //Default implementation, just returns first valid response, or if there are none, an ErrorResponse
  private[aggregation] def makeResponseFrom(validResponses: Iterable[Valid[T]],respondingTo: BroadcastMessage): BaseShrineResponse = {


    validResponses.map(_.response).toSet.headOption.getOrElse{
      val problem = NoValidResponsesToAggregate(respondingTo.request.requestType,respondingTo.networkAuthn.username,respondingTo.networkAuthn.domain)
      ErrorResponse(problem)
    }
  }
}

case class NoValidResponsesToAggregate(requestType: RequestType,userName:String,domain:String) extends AbstractProblem(ProblemSources.Hub) {
  override val summary: String = "No valid responses to aggregate."

  override val description:String = "The hub received no valid responses to aggregate."

  val internalSummary = s"The hub received no valid responses to aggregate while processing a $requestType for $userName:$domain"

  override val detailsXml: NodeSeq = NodeSeq.fromSeq(
    <details>
      {internalSummary}
      {throwableDetail.getOrElse("")}
    </details>
  )

}