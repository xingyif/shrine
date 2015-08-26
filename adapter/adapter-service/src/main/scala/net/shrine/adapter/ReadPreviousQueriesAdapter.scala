package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.log.Loggable
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.ReadPreviousQueriesRequest
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.client.HttpClient
import net.shrine.protocol.ShrineResponse

/**
 * @author Bill Simons
 * @author clint
 * @date 4/11/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class ReadPreviousQueriesAdapter(dao: AdapterDao) extends Adapter with Loggable {
  
  override protected[adapter] def processRequest(message: BroadcastMessage): ShrineResponse = {
    val fetchSize = message.request.asInstanceOf[ReadPreviousQueriesRequest].fetchSize
    
    val networkAuthn = message.networkAuthn
    
    val previousQueries = dao.findQueriesByUserAndDomain(networkAuthn.domain, networkAuthn.username, fetchSize)
    
    ReadPreviousQueriesResponse(previousQueries.map(_.toQueryMaster()))
  }
}
