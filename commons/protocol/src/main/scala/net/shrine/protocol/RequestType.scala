package net.shrine.protocol

import net.shrine.util.SEnum

/**
 * @author clint
 * @date Apr 2, 2013
 */
final case class RequestType private (name: String, crcRequestType: Option[CrcRequestType]) extends RequestType.Value

object RequestType extends SEnum[RequestType] {
  private def apply(crcRequestType: CrcRequestType): RequestType = new RequestType(crcRequestType.name, Option(crcRequestType))
  
  val SheriffRequest = RequestType("SheriffRequest", None)

  val GetQueryResult = RequestType("GetQueryResult", None)
  
  val ReadOntChildTerms = RequestType("ReadOntChildTerms", None)
  
  val ReadI2b2AdminPreviousQueriesRequest = RequestType("ReadI2b2AdminPreviousQueriesRequest", None)
  
  val ReadTranslatedQueryDefinitionRequest = RequestType("ReadTranslatedQueryDefinitionRequest", None)
  
  val FlagQueryRequest = RequestType("FlagQueryRequest", None)
  
  val UnFlagQueryRequest = RequestType("UnFlagQueryRequest", None)
  
  val RunHeldQueryRequest = RequestType("RunHeldQueryRequest", None)
  
  val ReadI2b2AdminQueryingUsers = RequestType("ReadI2b2AdminQueryingUsers", None)
  
  val GetPDOFromInputListRequest = RequestType(CrcRequestType.GetPDOFromInputListRequestType)
  
  val InstanceRequest = RequestType(CrcRequestType.InstanceRequestType)
  
  val MasterRequest = RequestType(CrcRequestType.MasterRequestType)
  
  val QueryDefinitionRequest = RequestType(CrcRequestType.QueryDefinitionRequestType)
  
  val UserRequest = RequestType(CrcRequestType.UserRequestType)
  
  val ResultRequest = RequestType(CrcRequestType.ResultRequestType)
  
  val MasterDeleteRequest = RequestType(CrcRequestType.MasterDeleteRequestType)
  
  val MasterRenameRequest = RequestType(CrcRequestType.MasterRenameRequestType)
  
  val GetRequestXml = RequestType(CrcRequestType.GetRequestXml)
  
  val GetResultOutputTypesRequest = RequestType(CrcRequestType.GetResultOutputTypes)
  
  private lazy val byCrcReqType: Map[CrcRequestType, RequestType] = {
    Map.empty ++ (for {
      reqType <- values
      crcReqType <- reqType.crcRequestType 
    } yield crcReqType -> reqType)
  }
  
  def withCrcRequestType(crcReqType: CrcRequestType): Option[RequestType] = byCrcReqType.get(crcReqType)
}

