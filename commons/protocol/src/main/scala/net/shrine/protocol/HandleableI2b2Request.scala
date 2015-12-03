package net.shrine.protocol

import CrcRequestType._
import scala.xml.NodeSeq

/**
 * @author clint
 * @date Feb 19, 2014
 */
trait HandleableI2b2Request { self: ShrineRequest =>
  def handleI2b2(handler: I2b2RequestHandler, shouldBroadcast: Boolean): ShrineResponse
}

object HandleableI2b2Request extends AbstractShrineRequestI2b2UnmarshallerCompanion[ShrineRequest with HandleableI2b2Request](
  Map(
    InstanceRequestType -> ReadInstanceResultsRequest,
    UserRequestType -> ReadPreviousQueriesRequest,
    GetRequestXml -> ReadQueryDefinitionRequest,
    MasterRequestType -> ReadQueryInstancesRequest,
    QueryDefinitionRequestType -> RunQueryRequest,
    MasterRenameRequestType -> RenameQueryRequest,
    MasterDeleteRequestType -> DeleteQueryRequest,
    GetResultOutputTypes -> ReadResultOutputTypesRequest))