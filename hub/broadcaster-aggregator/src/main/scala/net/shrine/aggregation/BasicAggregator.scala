package net.shrine.aggregation

import net.shrine.log.Loggable

import scala.concurrent.duration.Duration
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.Failure
import net.shrine.protocol.NodeId
import net.shrine.protocol.Result
import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.Timeout
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
 * Represents the basic aggregation strategy shared by several aggregators:
 *   - Parses a sequence of SpinResultEntries into a sequence of some
 *   combination of valid responses, ErrorResponses, and invalid
 *   responses (cases where ShrineResponse.fromXml returns None)
 *   - Filters the valid responses, weeding out responses that aren't of
 *   the expected type
 *   Invokes an abstract method with the valid responses, errors, and
 *   invalid responses.
 *
 * Needs to be an abstract class instead of a trait due to the view bound on T (: Manifest)
 */
abstract class BasicAggregator[T <: BaseShrineResponse: Manifest] extends Aggregator with Loggable {

  private[aggregation] def isAggregatable(response: BaseShrineResponse): Boolean = {
    manifest[T].runtimeClass.isAssignableFrom(response.getClass)
  }

  import BasicAggregator._

  override def aggregate(results: Iterable[SingleNodeResult], errors: Iterable[ErrorResponse]): BaseShrineResponse = {
    val resultsOrErrors: Iterable[ParsedResult[T]] = {
      for {
        result <- results
      } yield {
        val parsedResponse: ParsedResult[T] = result match {
          case Result(origin, _, errorResponse: ErrorResponse) => Error(Option(origin), errorResponse)
          case Result(origin, elapsed, response: T) if isAggregatable(response) => Valid(origin, elapsed, response)
          case Timeout(origin) => Error(Option(origin), ErrorResponse(s"Timed out querying node '${origin.name}'"))
            //todo failure becomes an ErrorResponse and Error status type here. And the stack trace gets eaten.
          case Failure(origin, cause) => Error(Option(origin), ErrorResponse(s"Failure querying node '${origin.name}': ${cause.getMessage}"))
          case _ => Invalid(None, s"Unexpected response in $getClass:\r\n $result")
        }

        parsedResponse
      }
    }

    val invalidResponses = resultsOrErrors.collect { case invalid: Invalid => invalid }

    val validResponses = resultsOrErrors.collect { case valid: Valid[T] => valid }

    val errorResponses: Iterable[Error] = resultsOrErrors.collect { case error: Error => error }

    //Log all parsing errors
    invalidResponses.map(_.errorMessage).foreach(this.error(_))

    val previouslyDetectedErrors = errors.map(Error(None, _))

    makeResponseFrom(validResponses, errorResponses ++ previouslyDetectedErrors, invalidResponses)
  }

  private[aggregation] def makeResponseFrom(validResponses: Iterable[Valid[T]], errorResponses: Iterable[Error], invalidResponses: Iterable[Invalid]): BaseShrineResponse
}

object BasicAggregator {
  private[aggregation] sealed abstract class ParsedResult[+T]

  private[aggregation] final case class Valid[T](origin: NodeId, elapsed: Duration, response: T) extends ParsedResult[T]
  private[aggregation] final case class Error(origin: Option[NodeId], response: ErrorResponse) extends ParsedResult[Nothing]
  private[aggregation] final case class Invalid(origin: Option[NodeId], errorMessage: String) extends ParsedResult[Nothing]
}