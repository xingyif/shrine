package net.shrine.adapter

import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.ShrineResponse

/**
 * @author clint
 * @date Jan 3, 2014
 */
class MockAdapter extends Adapter {
  @volatile var isShutdown = false
  
  override protected[adapter] def processRequest(message: BroadcastMessage): ShrineResponse = ???
  
  override def shutdown() {
    isShutdown = true
  }
}