package net.shrine.protocol

import CrcRequestType._
import scala.xml.NodeSeq
import scala.util.Try

/**
 * @author clint
 * @since Apr 17, 2013
 */
trait HandleableAdminShrineRequest {self: ShrineRequest =>
  def handleAdmin(handler: I2b2AdminRequestHandler, shouldBroadcast: Boolean): ShrineResponse
}

object HandleableAdminShrineRequest extends AbstractI2b2UnmarshallerCompanion[ShrineRequest with HandleableAdminShrineRequest](
  Map(GetRequestXml -> I2b2AdminReadQueryDefinitionRequest)) {

  //TODO: This conditional chain is a smell
  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(i2b2Request: NodeSeq): Try[ShrineRequest with HandleableAdminShrineRequest] = {
    if (isPsmRequest(i2b2Request)) {
      parsePsmRequest(breakdownTypes, i2b2Request)
    } else if (isI2b2AdminPreviousQueriesRequest(i2b2Request)) {
      ReadI2b2AdminPreviousQueriesRequest.fromI2b2(breakdownTypes)(i2b2Request)
    } else if (isI2b2AdminQueryingUsersRequest(i2b2Request)) {
      ReadI2b2AdminQueryingUsersRequest.fromI2b2(breakdownTypes)(i2b2Request)
    } else if (isRunHeldQueryRequest(i2b2Request)) {
      RunHeldQueryRequest.fromI2b2(breakdownTypes)(i2b2Request)
    } else {
      throw new Exception(s"Request not understood: $i2b2Request")
    }
  }

  protected def isPdoRequest(requestXml: NodeSeq): Boolean = hasMessageBodySubElement(requestXml, "pdoheader")

  protected def isSheriffRequest(requestXml: NodeSeq): Boolean = hasMessageBodySubElement(requestXml, "sheriff_header")

  protected def isI2b2AdminPreviousQueriesRequest(requestXml: NodeSeq): Boolean = hasMessageBodySubElement(requestXml, "get_name_info")
  
  protected def isI2b2AdminQueryingUsersRequest(requestXml: NodeSeq): Boolean = hasMessageBodySubElement(requestXml, "get_all_role")
  
  protected def isRunHeldQueryRequest(requestXml: NodeSeq): Boolean = hasMessageBodySubElement(requestXml, "runHeldQuery")

}