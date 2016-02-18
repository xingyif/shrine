package net.shrine.adapter

import xml.NodeSeq
import net.shrine.client.HttpClient
import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.{HiveCredentials, BroadcastMessage, ShrineResponse, RenameQueryResponse, RenameQueryRequest}

/**
 * @author Bill Simons
 * @date 4/11/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class RenameQueryAdapter(dao: AdapterDao) extends Adapter {
  override protected[adapter] def processRequest(message: BroadcastMessage): ShrineResponse = {
    val request = message.request.asInstanceOf[RenameQueryRequest]
    
    dao.renameQuery(request.networkQueryId, request.queryName)
    
    RenameQueryResponse(request.networkQueryId, request.queryName)
  }
}