package net.shrine.protocol

import net.shrine.serialization.XmlMarshaller

/**
 * @author clint
 * @date Nov 25, 2013
 */
trait ShrineMessage extends XmlMarshaller

/**
 * @author clint
 * @date Nov 25, 2013
 */
object ShrineMessage extends XmlUnmarshallers.Chained(
    _ => xml => BroadcastMessage.fromXml(xml), 
    BaseShrineRequest.fromXml _, 
    BaseShrineResponse.fromXml _)
