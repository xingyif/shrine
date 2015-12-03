package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.BaseShrineResponse
import net.shrine.protocol.UnFlagQueryRequest
import net.shrine.protocol.UnFlagQueryResponse

/**
 * @author clint
 * @date Apr 18, 2014
 */
final class UnFlagQueryAdapter(dao: AdapterDao) extends Adapter {
  override protected[adapter] def processRequest(message: BroadcastMessage): BaseShrineResponse = {
    val request = message.request.asInstanceOf[UnFlagQueryRequest]
    
    dao.unFlagQuery(request.networkQueryId)
    
    UnFlagQueryResponse
  }
}