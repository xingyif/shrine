package net.shrine.broadcaster.dao.squeryl

import net.shrine.broadcaster.dao.HubDao
import net.shrine.log.Loggable
import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.BroadcastMessage
import net.shrine.broadcaster.dao.squeryl.tables.Tables
import net.shrine.dao.squeryl.SquerylInitializer
import net.shrine.broadcaster.dao.model.squeryl.SquerylHubQueryRow
import net.shrine.dao.DateHelpers
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.query.QueryDefinition
import net.shrine.dao.squeryl.SquerylEntryPoint
import net.shrine.broadcaster.dao.model.squeryl.SquerylHubQueryResultRow
import java.sql.Timestamp
import net.shrine.protocol.Result
import net.shrine.protocol.Timeout
import net.shrine.protocol.Failure
import net.shrine.protocol.ErrorResponse
import net.shrine.broadcaster.dao.model.HubQueryStatus

/**
 * @author clint
 * @date Dec 11, 2014
 */
final class SquerylHubDao(initializer: SquerylInitializer, tables: Tables) extends HubDao with Loggable {
  initializer.init()

  override def inTransaction[T](f: => T): T = SquerylEntryPoint.inTransaction { f }

  override def logOutboundQuery(networkQueryId: Long, networkAuthn: AuthenticationInfo, queryDef: QueryDefinition): Unit = {
    val newRow = SquerylHubQueryRow(
      networkQueryId,
      networkAuthn.domain,
      networkAuthn.username,
      now,
      queryDef.toXmlString)
      
    inTransaction {
      tables.hubQueries.insert(newRow)
    }
  }

  override def logQueryResult(networkQueryId: Long, result: SingleNodeResult): Unit = {
    val newRow = toQueryResultRow(networkQueryId, result)
    
    inTransaction {
      tables.hubQueryResults.insert(newRow)
    }
  }

  private def now: Timestamp = DateHelpers.toTimestamp(XmlDateHelper.now)

  private[squeryl] def toStatus(result: SingleNodeResult): String = {
    import net.shrine.broadcaster.dao.model.{HubQueryStatus => hqs}
    
    val status = result match {
      case _: Failure => hqs.Failure
      case Result(_, _, e: ErrorResponse) => hqs.DownstreamFailure
      case _: Result => hqs.Success
      case _: Timeout => hqs.Timeout
      case _ => hqs.Unknown
    }

    status.name
  }

  private[squeryl] def toQueryResultRow(networkQueryId: Long, result: SingleNodeResult): SquerylHubQueryResultRow = {
    SquerylHubQueryResultRow(
      -1, //NB: Squeryl pushes us towards inserting with dummy ids
      networkQueryId,
      result.origin.name,
      now,
      toStatus(result))
  }
}