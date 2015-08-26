package net.shrine.service.audit

import net.shrine.audit.{QueryTopicId, Time, QueryName, NetworkQueryId, UserName, ShrineNodeId}

/**
 * Container for QEP audit data for ACT metrics
 * 
 * @author david 
 * @since 8/17/15
 */
case class QepQueryAuditData(shrineNodeId:ShrineNodeId,
                             userName:UserName,
                             networkQueryId:NetworkQueryId,
                             queryName:QueryName,
                             timeQuerySent:Time,
                             queryTopicId:Option[QueryTopicId]) {}

object QepQueryAuditData extends ((ShrineNodeId,UserName,NetworkQueryId,QueryName,Time,Option[QueryTopicId]) => QepQueryAuditData) {

//todo should be able to access the actId from the KeyStore.myCn, if you can find a way to get at it.

  def apply(
    shrineNodeId:String,
    userName:String,
    networkQueryId:Long,
    queryName:String,
    queryTopicId:Option[String]
    ):QepQueryAuditData = QepQueryAuditData(
      shrineNodeId,
      userName,
      networkQueryId,
      queryName,
      System.currentTimeMillis(),
      queryTopicId
    )
}