package net.shrine.adapter

import net.shrine.protocol.RequestType
import scala.util.Try

/**
 * @author clint
 * @date Nov 14, 2013
 */
final case class AdapterMap(requestsToAdapters: Map[RequestType, Adapter]) {
  def canHandle(requestType: RequestType): Boolean = requestsToAdapters.contains(requestType)
  
  def adapterFor(requestType: RequestType): Option[Adapter] = requestsToAdapters.get(requestType)
  
  def handleableRequestTypes: Iterable[RequestType] = requestsToAdapters.keys
  
  @volatile private[this] var shutdownFlag = false
  
  def shutdown(): Unit = {
    try { requestsToAdapters.values.foreach(adapter => Try(adapter.shutdown())) } 
    finally { shutdownFlag = true }
  }
  
  def isShutdown = shutdownFlag
  
  //TODO: Find a way to wire this up in a less side-effecty-way.  This is necessary (for now)
  //So that AdapterMapShutdownServletContextListener, which must have a no-arg constructor, can
  //find have a handle to shut down all created and registered AdapterMaps
  AdapterMap.register(this)
}

object AdapterMap {
  @volatile private[this] var instances = WeakSet.empty[AdapterMap]
  
  private[this] final val lock = new AnyRef
  
  def register(map: AdapterMap) {
    lock.synchronized { instances :+= map }
  }
  
  def shutdownAllMaps() {
    lock.synchronized { instances.foreach(_.shutdown()) }
  }
  
  def clearReferences() {
    lock.synchronized { instances = WeakSet.empty[AdapterMap] }
  }
  
  //NB: For Spring
  @Deprecated
  def apply(requestsToAdapters: java.util.Map[String, Adapter]): AdapterMap = {
    def fromString(r: String): RequestType = RequestType.valueOf(r).get //Fail loudly

    import scala.collection.JavaConverters._
    
    new AdapterMap(requestsToAdapters.asScala.toMap.map { case (r, a) => (fromString(r), a) })
  }
  
  def empty = new AdapterMap(Map.empty)
}

