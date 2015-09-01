package net.shrine.service.audit

import net.shrine.audit.{QueryTopicId, Time, QueryName, NetworkQueryId, UserName, ShrineNodeId}
import net.shrine.protocol.RunQueryRequest

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

  def fromRunQueryRequest(request:RunQueryRequest,commonName:String):QepQueryAuditData = {
    QepQueryAuditData(
      commonName,
      request.authn.username,
      request.networkQueryId,
      request.queryDefinition.name,
      request.topicIdAndName.map(_._1)) //todo pick up the topic name here
  }

}