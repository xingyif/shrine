package net.shrine.adapter

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit

import net.shrine.protocol.RequestType

/**
 * @author clint
 * @date Jan 2, 2014
 */
final class AdapterMapTest extends ShouldMatchersForJUnit {
  @Test
  def testCanHandle {
    val requestType = RequestType.GetQueryResult
    
    val map = AdapterMap(Map(requestType -> null))
    
    map.canHandle(requestType) should be(true)
    
    RequestType.values.filterNot(_ == requestType).foreach { rt =>
      map.canHandle(rt) should be(false)
    }
  }
  
  @Test
  def testAdapterFor {
    val requestType = RequestType.GetQueryResult
    
    val adapter = new MockAdapter
    
    val map = AdapterMap(Map(requestType -> adapter))
    
    map.adapterFor(requestType) should equal(Some(adapter))
    
    RequestType.values.filterNot(_ == requestType).foreach { rt =>
      map.adapterFor(rt) should be(None)
    }
  }
  
  @Test
  def testShutdown {
    val adapter1 = new MockAdapter
    val adapter2 = new MockAdapter
    val adapter3 = new MockAdapter
    
    val map = AdapterMap(Map(
        RequestType.GetPDOFromInputListRequest -> adapter1, 
        RequestType.GetQueryResult -> adapter2, 
        RequestType.GetRequestXml -> adapter3))
        
    map.isShutdown should be(false)
    adapter1.isShutdown should be(false)
    adapter2.isShutdown should be(false)
    adapter3.isShutdown should be(false)
    
    map.shutdown()
    
    map.isShutdown should be(true)
    adapter1.isShutdown should be(true)
    adapter2.isShutdown should be(true)
    adapter3.isShutdown should be(true)
  }
  
  @Test
  def testRegisterAndClearReferences {
    val map1 = AdapterMap.empty
    val map2 = AdapterMap.empty
    val map3 = AdapterMap.empty
    
    map1.isShutdown should be(false)
    map2.isShutdown should be(false)
    map3.isShutdown should be(false)
    
    AdapterMap.clearReferences()
    
    AdapterMap.shutdownAllMaps()
    
    map1.isShutdown should be(false)
    map2.isShutdown should be(false)
    map3.isShutdown should be(false)
    
    AdapterMap.register(map1)
    AdapterMap.register(map2)
    //Not map3
    
    AdapterMap.shutdownAllMaps()
    
    map1.isShutdown should be(true)
    map2.isShutdown should be(true)
    map3.isShutdown should be(false)
  }
  
  @Test
  def testCompanionObjectShutdownAllMaps {
    val map1 = AdapterMap.empty
    val map2 = AdapterMap.empty
    val map3 = AdapterMap.empty
    
    map1.isShutdown should be(false)
    map2.isShutdown should be(false)
    map3.isShutdown should be(false)
    
    AdapterMap.shutdownAllMaps()
    
    map1.isShutdown should be(true)
    map2.isShutdown should be(true)
    map3.isShutdown should be(true)
  }
  
  @Test
  def testCompanionObjectApply {
    val adapter: Adapter = new MockAdapter
    
    val rawMapping = Map(RequestType.GetQueryResult -> adapter)
    
    import scala.collection.JavaConverters._
    
    val rawStringMapping: java.util.Map[String, Adapter] = Map(RequestType.GetQueryResult.name -> adapter).asJava
    
    val map = AdapterMap(rawMapping)
    
    AdapterMap(rawMapping) should equal(AdapterMap(rawStringMapping))
  }
  
  @Test
  def testEmpty {
    val map = AdapterMap.empty
    
    RequestType.values.foreach { rt =>
      map.canHandle(rt) should be(false)
    }
  }
}