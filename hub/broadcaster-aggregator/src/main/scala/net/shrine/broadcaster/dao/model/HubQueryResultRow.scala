package net.shrine.broadcaster.dao.model

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.query.QueryDefinition

/**
 * @author clint
 * @date Dec 15, 2014
 */
final case class HubQueryResultRow(
    networkQueryId: Long,
    nodeName: String,
    timestamp: XMLGregorianCalendar,
    status: HubQueryStatus)