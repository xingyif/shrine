package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import CrcRequestType._
import scala.util.Try

/**
 * @author clint
 * @date Aug 15, 2012
 */
trait CrcRequest { self: ShrineRequest =>
  def crcRequestType: Option[CrcRequestType] = requestType.crcRequestType
  
  def i2b2PsmHeaderWithDomain: NodeSeq = makeI2b2PsmHeader(Option(authn.domain))
  
  def i2b2PsmHeader: NodeSeq = makeI2b2PsmHeader(None)
  
  private def makeI2b2PsmHeader(domainOption: Option[String] = None): NodeSeq = XmlUtil.stripWhitespace {
    <ns4:psmheader>
      {
        domainOption match {
          case Some(domain) => <user group={ authn.domain } login={ authn.username }>{ authn.username }</user>
          case None => <user login={ authn.username }>{ authn.username }</user>
        }
      }
      <patient_set_limit>0</patient_set_limit>
      <estimated_time>0</estimated_time>
      { crcRequestType.map(rt => <request_type> { rt.i2b2RequestType } </request_type>).orNull }
    </ns4:psmheader>
  }
}

object CrcRequest extends AbstractI2b2UnmarshallerCompanion[ShrineRequest with CrcRequest](
  Map(
    InstanceRequestType -> ReadInstanceResultsRequest,
    UserRequestType -> ReadPreviousQueriesRequest,
    GetRequestXml -> ReadQueryDefinitionRequest,
    MasterRequestType -> ReadQueryInstancesRequest,
    QueryDefinitionRequestType -> RunQueryRequest,
    MasterRenameRequestType -> RenameQueryRequest,
    MasterDeleteRequestType -> DeleteQueryRequest,
    ResultRequestType -> ReadResultRequest)) {

  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(i2b2Request: NodeSeq): Try[ShrineRequest with CrcRequest] = {
    if (isPsmRequest(i2b2Request)) {
      parsePsmRequest(breakdownTypes, i2b2Request)
    } else {
      scala.util.Failure(new Exception(s"Request not understood: $i2b2Request"))
    }
  }
}
 