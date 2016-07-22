package net.shrine.aggregation

import java.net.{UnknownHostException, ConnectException}

import com.sun.jersey.api.client.ClientHandlerException
import net.shrine.broadcaster.CouldNotParseResultsException
import net.shrine.log.Loggable
import net.shrine.problem.{ProblemNotYetEncoded, ProblemSources, AbstractProblem}

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
          case Timeout(origin) => Error(Option(origin), ErrorResponse(TimedOutWithAdapter(origin)))
          case Failure(origin, cause) => cause match {
            case cx: ConnectException => Error(Option(origin), ErrorResponse(CouldNotConnectToAdapter(origin, cx)))
            case uhx: UnknownHostException => Error(Option(origin), ErrorResponse(CouldNotConnectToAdapter(origin, uhx)))
            case chx: ClientHandlerException => Error(Option(origin), ErrorResponse(CouldNotConnectToAdapter(origin, chx)))
            case cnprx:CouldNotParseResultsException =>
              if(cnprx.statusCode >= 400) Error(Option(origin), ErrorResponse(HttpErrorResponseProblem(cnprx)))
              else Error(Option(origin), ErrorResponse(CouldNotParseResultsProblem(cnprx)))

            case x => Error(Option(origin), ErrorResponse(ProblemNotYetEncoded(s"Failure querying node ${origin.name}",x)))
          }
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

case class CouldNotConnectToAdapter(origin:NodeId,cx: Exception) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(cx)
  override val summary: String = "Shrine could not connect to the adapter."
  override val description: String = s"Shrine could not connect to the adapter at ${origin.name} due to ${throwable.get}."
}

case class TimedOutWithAdapter(origin:NodeId) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = None
  override val summary: String = "Timed out with adapter."
  override val description: String = s"Shrine observed a timeout with the adapter at ${origin.name}."
}

case class CouldNotParseResultsProblem(cnrpx:CouldNotParseResultsException) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(cnrpx)
  override val summary: String = "Could not parse response."
  override val description = s"While parsing a response from ${cnrpx.url} with http code ${cnrpx.statusCode} caught '${cnrpx.cause}'"
  override val detailsXml = <details>
                              Message body is {cnrpx.body}
                              {throwableDetail.getOrElse("")}
                            </details>
}

case class HttpErrorResponseProblem(cnrpx:CouldNotParseResultsException) extends AbstractProblem(ProblemSources.Hub) {
  override val throwable = Some(cnrpx)
  override val summary: String = "Adapter error."
  override val description = s"Observed http status code ${cnrpx.statusCode} from ${cnrpx.url} and caught ${cnrpx.cause}."
  override val detailsXml = <details>
                              Message body is {cnrpx.body}
                              {throwableDetail.getOrElse("")}
                            </details>

}