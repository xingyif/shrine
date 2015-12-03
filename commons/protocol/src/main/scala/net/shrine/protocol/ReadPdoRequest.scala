package net.shrine.protocol

import scala.concurrent.duration.Duration
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.transform.RewriteRule
import scala.xml.transform.RuleTransformer
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.util.XmlUtil
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author Bill Simons
 * @date 3/9/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class ReadPdoRequest(
  override val projectId: String,
  override val waitTime: Duration,
  override val authn: AuthenticationInfo,
  val patientSetCollId: String,
  val optionsXml: NodeSeq) extends ShrineRequest(projectId, waitTime, authn) with TranslatableRequest[ReadPdoRequest] with HandleableShrineRequest with HandleableI2b2Request {

  override val requestType = RequestType.GetPDOFromInputListRequest

  override def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean) = handler.readPdo(this, shouldBroadcast)

  override def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean) = handler.readPdo(this, shouldBroadcast)

  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <readPdo>
      { headerFragment }
      <optionsXml>
        { getOptionsXml }
      </optionsXml>
      <patientSetCollId>
        { patientSetCollId }
      </patientSetCollId>
    </readPdo>
  }

  private[protocol] def getOptionsXml: NodeSeq = ReadPdoRequest.updateCollId(optionsXml.head, patientSetCollId).toSeq

  protected override def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    <message_body>
      <ns3:pdoheader>
        <patient_set_limit>0</patient_set_limit>
        <estimated_time>180000</estimated_time>
        <request_type>getPDO_fromInputList</request_type>
      </ns3:pdoheader>
      { getOptionsXml }
    </message_body>
  }

  override def withAuthn(ai: AuthenticationInfo) = this.copy(authn = ai)

  override def withProject(proj: String) = this.copy(projectId = proj)

  def withPatientSetCollId(newPatientSetCollId: String) = this.copy(patientSetCollId = newPatientSetCollId)
}

object ReadPdoRequest extends I2b2XmlUnmarshaller[ReadPdoRequest] with ShrineXmlUnmarshaller[ReadPdoRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(nodeSeq: NodeSeq): Try[ReadPdoRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(nodeSeq)
      waitTime <- i2b2WaitTime(nodeSeq)
      authn <- i2b2AuthenticationInfo(nodeSeq)
      patientSetCollId <- (nodeSeq withChild "message_body" withChild "request" withChild "input_list" withChild "patient_list" withChild "patient_set_coll_id").map(_.text)
      requestXml <- (nodeSeq withChild "message_body" withChild "request")
    } yield {
      ReadPdoRequest(
        projectId,
        waitTime,
        authn,
        patientSetCollId,
        requestXml)
    }
  }

  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadPdoRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      patientSetCollId <- xml.withChild("patientSetCollId").map(_.text)
      projectId <- shrineProjectId(xml)
      optionsXml = xml \ "optionsXml" \ "request"
    } yield {
      ReadPdoRequest(projectId, waitTime, authn, patientSetCollId, optionsXml)
    }
  }

  def updateCollId(nodes: NodeSeq, patientSetCollId: String): Option[Node] = {
    object rule extends RewriteRule {
      override def transform(n: Node): Seq[Node] = n match {
        case Elem(prefix, "patient_set_coll_id", attribs, scope, _*) => {
          //def apply(prefix: String, label: String, attributes: MetaData, scope: NamespaceBinding, minimizeEmpty: Boolean, child: Node*): Elem =
          Elem(prefix, "patient_set_coll_id", attribs, scope, false, Text(patientSetCollId))
        }
        case other => other
      }
    }

    val transformed = NodeSeq.Empty ++ (new RuleTransformer(rule)).transform(nodes)

    transformed.headOption
  }
}
