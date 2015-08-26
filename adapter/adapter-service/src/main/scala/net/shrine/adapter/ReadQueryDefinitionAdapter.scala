package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.adapter.components.QueryDefinitions

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
final class ReadQueryDefinitionAdapter(dao: AdapterDao) extends Adapter {

  private lazy val queryDefinitions = QueryDefinitions[ReadQueryDefinitionRequest](dao)
  
  override protected[adapter] def processRequest(message: BroadcastMessage): ShrineResponse = {
    val request = message.request.asInstanceOf[ReadQueryDefinitionRequest]

    queryDefinitions.get(request)
  }
}