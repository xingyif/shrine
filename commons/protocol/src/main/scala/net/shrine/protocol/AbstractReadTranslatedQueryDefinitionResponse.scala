package net.shrine.protocol

import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.protocol.query.QueryDefinition
import net.shrine.serialization.XmlUnmarshaller
import net.shrine.util.NodeSeqEnrichments.Strictness.HasStrictNodeSeqEnrichments
import net.shrine.util.{Tries, XmlUtil, NodeSeqEnrichments}
import scala.util.control.NonFatal

abstract class AbstractReadTranslatedQueryDefinitionResponse(rootTagName: String) extends NonI2b2ShrineResponse {
  import AbstractReadTranslatedQueryDefinitionResponse._

  def translated: Seq[SingleNodeTranslationResult]

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    XmlUtil.renameRootTag(rootTagName) {
      <placeholder>
        {
          if (translated.isEmpty) { <noTranslationResults/> }
          else { translated.map(_.toXml) }
        }
      </placeholder>
    }
  }
}

object AbstractReadTranslatedQueryDefinitionResponse {
  abstract class Companion[R <: AbstractReadTranslatedQueryDefinitionResponse](createResponse: Seq[SingleNodeTranslationResult] => R) extends XmlUnmarshaller[Try[R]] with HasRootTagName {
    def Empty = createResponse(Seq.empty)

    override def fromXml(xml: NodeSeq): Try[R] = {

      import NodeSeqEnrichments.Strictness._

      xml.withChild("noTranslationResults").map(_ => Empty).recoverWith {
        case NonFatal(e) =>
          for {
            resultXml <- xml withChild SingleNodeTranslationResult.rootTagName
            resultsAttempt = Tries.sequence(resultXml.map(SingleNodeTranslationResult.fromXml))
            results <- resultsAttempt
          } yield {
            createResponse(results)
          }
      }
    }
  }
}