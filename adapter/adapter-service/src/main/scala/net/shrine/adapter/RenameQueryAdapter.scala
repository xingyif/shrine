package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.{BroadcastMessage, ShrineResponse, RenameQueryResponse, RenameQueryRequest}

/**
 * @author Bill Simons
 * @since 4/11/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class RenameQueryAdapter(dao: AdapterDao) extends Adapter {
  override protected[adapter] def processRequest(message: BroadcastMessage): ShrineResponse = {
    val request = message.request.asInstanceOf[RenameQueryRequest]
    
    dao.renameQuery(request.networkQueryId, request.queryName)
    
    RenameQueryResponse(request.networkQueryId, request.queryName)
  }
}