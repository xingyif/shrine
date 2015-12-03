package net.shrine.broadcaster.dao.model

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.query.QueryDefinition

/**
 * @author clint
 * @date Dec 16, 2014
 */
final case class HubQueryRow(
    networkQueryId: Long,
    domain: String,
    username: String,
    time: XMLGregorianCalendar,
    queryDefinition: QueryDefinition)