package net.shrine.authorization

import net.shrine.util.SEnum

/**
 * @author clint
 * @since Jul 1, 2014
 */
final case class AuthorizationType private (name: String) extends AuthorizationType.Value

object AuthorizationType extends SEnum[AuthorizationType] {
  val ShrineSteward = AuthorizationType("shrine-steward")
  val HmsSteward = AuthorizationType("hms-steward")
  val NoAuthorization = AuthorizationType("none")
}