package net.shrine.hms.authorization

import net.shrine.authorization.AuthorizationResult
import net.shrine.protocol.ApprovedTopic

/**
 * @author clint
 * @since Apr 3, 2014
 */
trait SheriffClient {
  def getApprovedEntries(ecommonsUsername: String): Seq[ApprovedTopic]
  
  def isAuthorized(user: String, topicId: String, queryText: String): AuthorizationResult
}