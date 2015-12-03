package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.xml.NodeBuffer

/**
 * @author clint
 * @date Apr 18, 2014
 */
trait HasHeaderFields {
  def projectId: String 
  def waitTime: Duration 
  def authn: AuthenticationInfo
  
  final protected def headerFragment: NodeBuffer = <projectId>{ projectId }</projectId><waitTimeMs>{ waitTime.toMillis }</waitTimeMs> &+ authn.toXml
}