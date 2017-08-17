package net.shrine.protocol

import net.shrine.util.{Tries, XmlUtil, NodeSeqEnrichments, OptionEnrichments}
import net.shrine.protocol.query.QueryDefinition
import scala.xml.NodeSeq
import scala.xml.Elem
import scala.concurrent.duration.Duration
import scala.util.Try
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author Bill Simons
 * @since 3/9/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class RunQueryRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  networkQueryId: Long,
  topicId: Option[String], //data steward when required only, must be separate from topicName because HMS DSA does not supply topic names.
  topicName: Option[String], //data steward when required only
  outputTypes: Set[ResultOutputType],
  queryDefinition: QueryDefinition
  //todo pick up SHRINE-2174 here nodeId: Option[NodeId] = None
  ) extends ShrineRequest(projectId, waitTime, authn) with CrcRequest with TranslatableRequest[RunQueryRequest] with HandleableShrineRequest with HandleableI2b2Request {

  override val requestType = RequestType.QueryDefinitionRequest

  override def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean) = handler.runQuery(this, shouldBroadcast)

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean) = handler.runQuery(this, shouldBroadcast)

  def elideAuthenticationInfo: RunQueryRequest = copy(authn = AuthenticationInfo.elided)

  //NB: Sort ResultOutputTypes, for deterministic testing
  private def sortedOutputTypes: Seq[ResultOutputType] = outputTypes.toSeq.sortBy(_.name)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    import OptionEnrichments._

    <runQuery>
      { headerFragment }
      <queryId>{ networkQueryId }</queryId>
      { topicId.toXml(<topicId/>) }
      { topicName.toXml(<topicName/>) }
      <outputTypes>
        { sortedOutputTypes.map(_.toXml) }
      </outputTypes>
      { queryDefinition.toXml }
    </runQuery>
  } //todo put       { nodeId.map(_.toXml) } at the end for SHRINE-2174

  protected override def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    import OptionEnrichments._

    <message_body>
      { i2b2PsmHeaderWithDomain }
      <ns4:request xsi:type="ns4:query_definition_requestType" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        { queryDefinition.toI2b2 }
        <result_output_list>
          {
            for {
              (outputType, i) <- sortedOutputTypes.zipWithIndex
              priorityIndex = outputType.id.getOrElse(i + 1)
            } yield {
              <result_output priority_index={ priorityIndex.toString } name={ outputType.toString.toLowerCase }/>
            }
          }
        </result_output_list>
      </ns4:request>
      <shrine>{ topicId.toXml(<queryTopicID/>) }</shrine>
      <shrine>{ topicName.toXml(<queryTopicName/>) }</shrine>
    </message_body>
  }

  override def withProject(proj: String) = this.copy(projectId = proj)

  override def withAuthn(ai: AuthenticationInfo) = this.copy(authn = ai)

  def withQueryDefinition(qDef: QueryDefinition) = this.copy(queryDefinition = qDef)

  def mapQueryDefinition(f: QueryDefinition => QueryDefinition) = this.withQueryDefinition(f(queryDefinition))

  def withNetworkQueryId(id: Long) = this.copy(networkQueryId = id)
}

object RunQueryRequest extends I2b2XmlUnmarshaller[RunQueryRequest] with ShrineXmlUnmarshaller[RunQueryRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {

  def apply(projectId: String,
  waitTime: Duration,
  authn: AuthenticationInfo,
  topicId: Option[String], //data steward when required only, must be separate from topicName because HMS DSA does not supply topic names.
  topicName: Option[String], //data steward when required only
  outputTypes: Set[ResultOutputType],
  queryDefinition: QueryDefinition
             ):RunQueryRequest = RunQueryRequest(
                                                  projectId,
                                                  waitTime,
                                                  authn,
                                                  BroadcastMessage.Ids.next,
                                                  topicId,
                                                  topicName,
                                                  outputTypes,
                                                  queryDefinition
                                                )

  val neededI2b2Namespace = "http://www.i2b2.org/xsd/cell/crc/psm/1.1/"

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[RunQueryRequest] = {
    val queryDefNode = xml \ "message_body" \ "request" \ "query_definition"

    val queryDefXml = queryDefNode.head match {
      //NB: elem.scope.getPrefix(neededI2b2Namespace) will return null if elem isn't part of a larger XML chunk that has
      //the http://www.i2b2.org/xsd/cell/crc/psm/1.1/ declared
      case elem: Elem => elem.copy(elem.scope.getPrefix(neededI2b2Namespace))
      case _ => throw new Exception("When unmarshalling a RunQueryRequest, encountered unexpected XML: '" + queryDefNode + "', <query_definition> might be missing.")
    }

    val attempt = for {
      projectId <- i2b2ProjectId(xml)
      waitTime <- i2b2WaitTime(xml)
      authn <- i2b2AuthenticationInfo(xml)
      topicId = (xml \ "message_body" \ "shrine" \ "queryTopicID").headOption.map(XmlUtil.trim)
      topicName = (xml \ "message_body" \ "shrine" \ "queryTopicName").headOption.map(XmlUtil.trim)
      outputTypes = determineI2b2OutputTypes(breakdownTypes)(xml \ "message_body" \ "request" \ "result_output_list")
      queryDef <- QueryDefinition.fromI2b2(queryDefXml)
    } yield {
      RunQueryRequest(
        projectId,
        waitTime,
        authn,
        topicId,
        topicName,
        outputTypes,
        queryDef
        //None //todo inject the QEP's nodeId here for SHRINE-2120
      )
    }
    
    attempt.map(addPatientCountXmlIfNecessary)
  }

  private def determineI2b2OutputTypes(breakdownTypes: Set[ResultOutputType])(nodeSeq: NodeSeq): Set[ResultOutputType] = {
    val sequence = (nodeSeq \ "result_output").flatMap { breakdownXml =>
      val breakdownName = XmlUtil.trim(breakdownXml \ "@name")

      ResultOutputType.valueOf(breakdownTypes)(breakdownName)
    }

    sequence.toSet
  }

  private def determineShrineOutputTypes(nodeSeq: NodeSeq): Set[ResultOutputType] = {
    val attempts = (nodeSeq \ "resultType").map(ResultOutputType.fromXml)

    Tries.sequence(attempts).map(_.toSet).get
  }

  private[protocol] def addPatientCountXmlIfNecessary(req: RunQueryRequest): RunQueryRequest = {
    import ResultOutputType.PATIENT_COUNT_XML
    
    if (req.outputTypes.contains(PATIENT_COUNT_XML)) { req }
    else { req.copy(outputTypes = req.outputTypes + PATIENT_COUNT_XML) }
  }

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[RunQueryRequest] = {
    import NodeSeqEnrichments.Strictness._

    val attempt = for {
      projectId <- shrineProjectId(xml)
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      queryId <- xml.withChild("queryId").map(XmlUtil.toLong)
      topicId = (xml \ "topicId").headOption.map(XmlUtil.trim)
      topicName = (xml \ "topicName").headOption.map(XmlUtil.trim)
      outputTypes <- xml.withChild("outputTypes").map(determineShrineOutputTypes)
      queryDef <- xml.withChild(QueryDefinition.rootTagName).flatMap(QueryDefinition.fromXml)
//      nodeId <- NodeId.fromXml(xml \ "nodeId")
    } yield {
      RunQueryRequest(projectId, waitTime, authn, queryId, topicId, topicName, outputTypes, queryDef)//, Some(nodeId))
    }

    attempt.map(addPatientCountXmlIfNecessary)
  }
}