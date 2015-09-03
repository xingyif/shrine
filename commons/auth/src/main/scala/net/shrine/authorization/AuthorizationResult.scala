package net.shrine.authorization

/**
 * @author clint
 * @since Dec 13, 2013
 */
sealed abstract class AuthorizationResult(val isAuthorized: Boolean)

object AuthorizationResult {
  final case class Authorized(topicIdAndName:Option[(String,String)]) extends AuthorizationResult(true)

  final case class NotAuthorized(reason: String) extends AuthorizationResult(false) {
    def toException = new AuthorizationException(reason)
  }
}