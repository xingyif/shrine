package net.shrine.protocol

import net.shrine.serialization.XmlUnmarshaller
import scala.xml.NodeSeq
import scala.util.Try
import scala.util

/**
 * @author clint
 * @since Feb 13, 2014
 */
trait NonI2b2ShrineResponse extends BaseShrineResponse

object NonI2b2ShrineResponse {
  def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[NonI2b2ShrineResponse] = {
    val rootTagName: String = xml.head.label
    
    unmarshallers.get(rootTagName).map(_.fromXml(xml)).getOrElse {
      util.Failure(new RootTagNotFoundException(rootTagName,xml,unmarshallers.keySet.map(_.toString)))
    }
  }
  
  private val unmarshallers: Map[String, Unmarshaller] = {
    Seq(SingleNodeReadTranslatedQueryDefinitionResponse,
        AggregatedReadTranslatedQueryDefinitionResponse).map(toMapping).toMap
  }
  
  private type Unmarshaller = XmlUnmarshaller[Try[_ <: NonI2b2ShrineResponse]]
  
  private def toMapping(companion: Unmarshaller with HasRootTagName): (String, Unmarshaller) = {
    companion.rootTagName -> companion
  }
}

case class RootTagNotFoundException(rootTagName: String,xml:NodeSeq,possibleRootTags:Set[String])
  extends Exception(s"Didn't recognize root tag name '$rootTagName' in XML '$xml'; supported tag names are ${possibleRootTags.mkString(",")}")