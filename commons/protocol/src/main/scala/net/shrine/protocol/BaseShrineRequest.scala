package net.shrine.protocol

import scala.concurrent.duration.Duration


/**
 * @author clint
 * @since Feb 14, 2014
 */
trait BaseShrineRequest extends ShrineMessage {
  def authn: AuthenticationInfo
  def waitTime: Duration
  
  def requestType: RequestType
  //todo maybe add a request-originated-from optional field here?
}

object BaseShrineRequest extends XmlUnmarshallers.Chained(ShrineRequest.fromXml, NonI2b2ShrineRequest.fromXml)
