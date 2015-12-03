package net.shrine.service.dao.model

import java.sql.Timestamp

/**
 * @author ???
 * @author clint
 * @date Jan 25, 2013
 */
//NB: Needs to be non-final for Squeryl :\
case class AuditEntry(
    id: Long,
    project: String, 
    domain: String,
    username: String,
    time: Timestamp, 
    queryText: Option[String], 
    queryTopic: Option[String])
