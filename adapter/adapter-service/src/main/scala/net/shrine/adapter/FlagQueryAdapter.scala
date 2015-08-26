package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.BaseShrineResponse
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.FlagQueryRequest
import net.shrine.protocol.FlagQueryResponse

/**
 * @author clint
 * @date Mar 26, 2014
 */
final class FlagQueryAdapter(dao: AdapterDao) extends Adapter {
  override protected[adapter] def processRequest(message: BroadcastMessage): BaseShrineResponse = {
    val request = message.request.asInstanceOf[FlagQueryRequest]
    
    dao.flagQuery(request.networkQueryId, request.message)
    
    FlagQueryResponse
  }
}
