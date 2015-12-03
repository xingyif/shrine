package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.ShrineResponse

/**
 * @author Bill Simons
 * @date 4/12/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class DeleteQueryAdapter(dao: AdapterDao) extends Adapter {
  override protected[adapter] def processRequest(message: BroadcastMessage): ShrineResponse = {
    val request = message.request.asInstanceOf[DeleteQueryRequest]
    
    dao.deleteQuery(request.queryId)
    
    DeleteQueryResponse(request.queryId)
  }
}