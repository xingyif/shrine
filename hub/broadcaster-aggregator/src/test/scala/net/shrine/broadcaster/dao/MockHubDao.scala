package net.shrine.broadcaster.dao

import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.SingleNodeResult

/**
 * @author clint
 * @date Dec 15, 2014
 */
object MockHubDao extends HubDao {
  override def inTransaction[T](f: => T): T = f
  
  override def logOutboundQuery(networkQueryId: Long, networkAuthn: AuthenticationInfo, queryDef: QueryDefinition): Unit = ()
  
  override def logQueryResult(networkQueryId: Long, result: SingleNodeResult): Unit = ()
}