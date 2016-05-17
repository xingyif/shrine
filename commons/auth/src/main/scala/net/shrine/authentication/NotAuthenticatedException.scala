package net.shrine.authentication

import net.shrine.authentication.AuthenticationResult.NotAuthenticated
import net.shrine.problem.{ProblemSources, AbstractProblem}

import scala.xml.NodeSeq

/**
 * @author clint
 * @since Dec 13, 2013
 */
final case class NotAuthenticatedException(domain: String, username: String,message: String, cause: Throwable) extends RuntimeException(message, cause) {

  def problem = NotAuthenticatedProblem(this)

}

object NotAuthenticatedException {

  def apply(na:NotAuthenticated):NotAuthenticatedException = NotAuthenticatedException(na.domain,na.username,na.message,na.cause.getOrElse(null))

}

case class NotAuthenticatedProblem(nax:NotAuthenticatedException) extends AbstractProblem(ProblemSources.Qep){
  override val summary = s"Can not authenticate ${nax.domain}:${nax.username}."

  override val throwable = Some(nax)

  override val description = s"Can not authenticate ${nax.domain}:${nax.username}. ${nax.getLocalizedMessage}"

  override val detailsXml: NodeSeq = NodeSeq.fromSeq(
    <details>
      {throwableDetail.getOrElse("")}
    </details>
  )
}