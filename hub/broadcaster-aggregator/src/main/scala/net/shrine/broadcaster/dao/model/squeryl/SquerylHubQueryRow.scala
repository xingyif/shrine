package net.shrine.broadcaster.dao.model.squeryl

import java.sql.Timestamp
import net.shrine.protocol.query.QueryDefinition
import org.squeryl.KeyedEntity
import net.shrine.broadcaster.dao.model.HubQueryResultRow
import net.shrine.dao.DateHelpers
import net.shrine.broadcaster.dao.model.HubQueryRow
import org.squeryl.annotations.Column

/**
 * @author clint
 * @date Dec 11, 2014
 */
//NB: This must NOT be final, due to Squeryl :(
case class SquerylHubQueryRow(
    @Column("NETWORK_QUERY_ID")
    id: Long,
    @Column("DOMAIN")
    domain: String,
    @Column("USERNAME")
    username: String,
    @Column("CREATE_DATE")
    time: Timestamp,
    @Column("QUERY_DEFINITION")
    queryDefinition: String) extends KeyedEntity[Long] {
  
  final def networkQueryId = id
  
  final def toHubQueryRow: HubQueryRow = {
    //NB: Fail fast when unmarshalling querydef xml
    HubQueryRow(networkQueryId, domain, username, DateHelpers.toXmlGc(time), QueryDefinition.fromXml(queryDefinition).get)
  }
}