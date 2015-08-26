package net.shrine.broadcaster.dao.model.squeryl

import java.sql.Timestamp
import net.shrine.protocol.CertId
import org.squeryl.KeyedEntity
import net.shrine.broadcaster.dao.model.HubQueryResultRow
import net.shrine.dao.DateHelpers
import org.squeryl.annotations.Column
import net.shrine.broadcaster.dao.model.HubQueryStatus
/**
 * @author clint
 * @date Dec 11, 2014
 */
//NB: This must NOT be final, due to Squeryl :(
//NB: This must not be final, due to Squeryl
case class SquerylHubQueryResultRow(
    //NB: Squeryl apparently needs an int primary key here; 
    //a composite key made from (networkQueryId, nodeName) did not work :(
    @Column("ID")
    id: Int,
    @Column("NETWORK_QUERY_ID")
    networkQueryId: Long,
    @Column("NODE_NAME")
    nodeName: String,
    @Column("CREATE_DATE")
    time: Timestamp,
    @Column("STATUS")
    status: String /*See model.HubQueryTimeout*/) extends KeyedEntity[Int] {
  
  final def toHubQueryResultRow: HubQueryResultRow = {
    val hubQueryStatus = HubQueryStatus.valueOf(status).getOrElse(HubQueryStatus.Unknown)
    
    HubQueryResultRow(networkQueryId, nodeName, DateHelpers.toXmlGc(time), hubQueryStatus)
  }
}