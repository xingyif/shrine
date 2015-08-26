package net.shrine.protocol

import net.shrine.protocol.query.QueryDefinition
import net.shrine.util.XmlUtil
import scala.xml.NodeSeq
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import scala.concurrent.duration.Duration
import net.shrine.util.DurationEnrichments._

/**
 * @author clint
 * @date Feb 13, 2014
 */
final case class ReadTranslatedQueryDefinitionRequest(authn: AuthenticationInfo, waitTime: Duration, queryDef: QueryDefinition) extends NonI2b2ShrineRequest {
  override val requestType = RequestType.ReadTranslatedQueryDefinitionRequest
  
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <readTranslatedQueryDefinitionRequest>
	  { authn.toXml }
	  { XmlUtil.renameRootTag("waitTime")(waitTime.toXml) }
      { queryDef.toXml }
    </readTranslatedQueryDefinitionRequest>
  }
}

object ReadTranslatedQueryDefinitionRequest extends ShrineXmlUnmarshaller[ReadTranslatedQueryDefinitionRequest] {
  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadTranslatedQueryDefinitionRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      authn <- xml.withChild(AuthenticationInfo.shrineXmlTagName).flatMap(AuthenticationInfo.fromXml)
      waitTime <- xml.withChild("waitTime")flatMap(Duration.fromXml)
      queryDef <- xml.withChild(QueryDefinition.rootTagName).flatMap(QueryDefinition.fromXml)
    } yield {
      ReadTranslatedQueryDefinitionRequest(authn, waitTime, queryDef)
    }
  }
}