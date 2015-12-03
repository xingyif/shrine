package net.shrine.protocol

import scala.concurrent.duration.Duration


/**
 * @author clint
 * @date Feb 14, 2014
 */
trait BaseShrineRequest extends ShrineMessage {
  def authn: AuthenticationInfo
  def waitTime: Duration
  
  def requestType: RequestType
}

object BaseShrineRequest extends XmlUnmarshallers.Chained(ShrineRequest.fromXml _, NonI2b2ShrineRequest.fromXml _)
