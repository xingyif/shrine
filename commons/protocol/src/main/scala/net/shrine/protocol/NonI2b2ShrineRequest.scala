package net.shrine.protocol

import scala.util.Try
import scala.xml.NodeSeq

/**
 * @author clint
 * @date Feb 18, 2014
 */
trait NonI2b2ShrineRequest extends BaseShrineRequest

object NonI2b2ShrineRequest extends XmlUnmarshallers.Chained[NonI2b2ShrineRequest](
    ReadTranslatedQueryDefinitionRequest.fromXml _, 
    ReadQueryResultRequest.fromXml _)
