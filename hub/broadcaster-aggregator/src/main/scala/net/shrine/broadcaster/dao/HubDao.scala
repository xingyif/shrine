package net.shrine.broadcaster.dao

import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.query.QueryDefinition

/**
 * @author clint
 * @date Dec 11, 2014
 */
trait HubDao {
  def inTransaction[T](f: => T): T
  
  def logOutboundQuery(networkQueryId: Long, networkAuthn: AuthenticationInfo, queryDef: QueryDefinition): Unit
  
  def logQueryResult(networkQueryId: Long, result: SingleNodeResult): Unit
}