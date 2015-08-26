package net.shrine.protocol

import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.XmlUtil
import scala.xml.NodeSeq
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments

/**
 * @author clint
 * @date Feb 13, 2014
 */
final case class SingleNodeTranslationResult(nodeId: NodeId, queryDef: QueryDefinition) {
  def toXml: NodeSeq = XmlUtil.stripWhitespace {
    import SingleNodeTranslationResult._

    XmlUtil.renameRootTag(rootTagName) {
      <translationResult>
        { nodeId.toXml }
        { queryDef.toXml }
      </translationResult>
    }
  }
}

object SingleNodeTranslationResult extends XmlUnmarshaller[Try[SingleNodeTranslationResult]] {
  val rootTagName = "translationResult"

  override def fromXml(xml: NodeSeq): Try[SingleNodeTranslationResult] = {
    import NodeSeqEnrichments.Strictness._

    for {
      nodeIdXml <- xml withChild NodeId.rootTagName
      queryDefXml <- xml withChild QueryDefinition.rootTagName
      nodeId <- NodeId.fromXml(nodeIdXml)
      queryDef <- QueryDefinition.fromXml(queryDefXml)
    } yield {
      SingleNodeTranslationResult(nodeId, queryDef)
    }
  }
}