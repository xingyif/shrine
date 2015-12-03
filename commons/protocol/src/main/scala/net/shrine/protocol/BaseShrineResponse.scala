package net.shrine.protocol

/**
 * @author clint
 * @date Feb 14, 2014
 */
trait BaseShrineResponse extends ShrineMessage

object BaseShrineResponse extends XmlUnmarshallers.Chained(ShrineResponse.fromXml _, NonI2b2ShrineResponse.fromXml _)
