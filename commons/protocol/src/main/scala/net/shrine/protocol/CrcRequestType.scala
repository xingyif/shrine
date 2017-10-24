package net.shrine.protocol

import net.shrine.util.SEnum

/**
 * Simple enum listing the types of CRC requests that Shrine knows how to
 * handle. The enum names are deliberately the same as the jaxb generated class
 * of the request.
 *
 * @author Justin Quan
 * @author clint
 * @date Jun 14, 2010
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 * <p/>
 * NOTICE: This software comes with NO guarantees whatsoever and is
 * licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final case class CrcRequestType private (name: String, i2b2RequestType: String) extends CrcRequestType.Value

//TODO: Rename these, add unit test

object CrcRequestType extends SEnum[CrcRequestType] {
  val GetPDOFromInputListRequestType = CrcRequestType("GetPDOFromInputListRequestType", "getPDO_fromInputList")

  //this might be the right one
  val InstanceRequestType = CrcRequestType("InstanceRequestType", "CRC_QRY_getQueryResultInstanceList_fromQueryInstanceId")
  
  val MasterRequestType = CrcRequestType("MasterRequestType", "CRC_QRY_getQueryInstanceList_fromQueryMasterId")
  
  val QueryDefinitionRequestType = CrcRequestType("QueryDefinitionRequestType", "CRC_QRY_runQueryInstance_fromQueryDefinition")
  
  val UserRequestType = CrcRequestType("UserRequestType", "CRC_QRY_getQueryMasterList_fromUserId")

  //if no results are available, the CRC reports that it does not know anything about the query, in plain text. See SHRINE-2115
  val ResultRequestType = CrcRequestType("ResultRequestType", "CRC_QRY_getResultDocument_fromResultInstanceId")
  
  val MasterDeleteRequestType = CrcRequestType("MasterDeleteRequestType", "CRC_QRY_deleteQueryMaster")
  
  val MasterRenameRequestType = CrcRequestType("MasterRenameRequestType", "CRC_QRY_renameQueryMaster")
  
  val GetRequestXml = CrcRequestType("GetRequestXml", "CRC_QRY_getRequestXml_fromQueryMasterId")

  val GetResultOutputTypes = CrcRequestType("GetResultOutputTypes", "CRC_QRY_getResultType")
  
  private lazy val byI2b2RequestType: Map[String, CrcRequestType] = {
    Map.empty ++ values.map(v => (v.i2b2RequestType -> v))
  }
  
  def withI2b2RequestType(i2b2ReqType: String): Option[CrcRequestType] = byI2b2RequestType.get(i2b2ReqType)
}
