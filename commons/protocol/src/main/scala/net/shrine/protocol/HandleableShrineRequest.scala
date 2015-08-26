package net.shrine.protocol

import CrcRequestType._
import scala.xml.NodeSeq

/**
 * @author clint
 * @date Apr 17, 2013
 */
trait HandleableShrineRequest { self: ShrineRequest =>
  def handle(handler: ShrineRequestHandler, shouldBroadcast: Boolean): BaseShrineResponse
}

object HandleableShrineRequest extends AbstractShrineRequestI2b2UnmarshallerCompanion[ShrineRequest with HandleableShrineRequest](
  Map(
    InstanceRequestType -> ReadInstanceResultsRequest,
    UserRequestType -> ReadPreviousQueriesRequest,
    GetRequestXml -> ReadQueryDefinitionRequest,
    MasterRequestType -> ReadQueryInstancesRequest,
    QueryDefinitionRequestType -> RunQueryRequest,
    MasterRenameRequestType -> RenameQueryRequest,
    MasterDeleteRequestType -> DeleteQueryRequest))