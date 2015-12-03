package net.shrine.steward.pmauth

import net.shrine.i2b2.protocol.pm.User

import net.shrine.authorization.steward.{stewardRole,qepRole}

/**
 * Authorizes for the data steward.
 *
 * @author david 
 * @since 8/11/15
 */
object Authorizer {

  def authorizeResearcher(user:User):Boolean = true //anyone I2B2 knows about is a researcher

  //todo look at PM project vs global params
  def authorizeSteward(user:User):Boolean = {
    user.params.toList.contains((stewardRole,"true"))
  }

  def authorizeQep(user:User):Boolean = {
    user.params.toList.contains((qepRole,"true"))
  }

}
