package net.shrine.war

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.adapter.AdapterMap
import net.shrine.adapter.Adapter
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.RequestType
import net.shrine.adapter.MockAdapter

/**
 * @author clint
 * @date Jan 2, 2014
 */
final class AdapterMapShutdownServletContextListenerTest extends ShouldMatchersForJUnit {
  @Test
  def testContextDestroyedEmptyAdapterMap {
    val map = AdapterMap.empty
    
    map.isShutdown should be(false)
    
    (new AdapterMapShutdownServletContextListener).contextDestroyed(null)
    
    map.isShutdown should be(true)
  }
  
  @Test
  def testContextDestroyed {
    val adapter = new MockAdapter
    
    val map = AdapterMap(Map(RequestType.GetQueryResult -> adapter))
    
    map.isShutdown should be(false)
    adapter.isShutdown should be(false)
    
    (new AdapterMapShutdownServletContextListener).contextDestroyed(null)
    
    map.isShutdown should be(true)
    adapter.isShutdown should be(true)
  }
  
  @Test
  def testContextInitialized {
    //NB: The best we can do is verify that this doesn't throw; we can't prove it doesn't have other side-effects. 
    (new AdapterMapShutdownServletContextListener).contextInitialized(null)
  }
}
