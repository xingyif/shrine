package net.shrine.protocol

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.query.QueryDefinition

/**
 * @author Bill Simons
 * @date 4/15/11
 * @link http://cbmi.med.harvard.edu
 * 
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 */
final case class RunQueryResponse(
    override val queryId: Long,
    override val createDate: XMLGregorianCalendar,
    override val userId: String,
    override val groupId: String,
    override val requestXml: QueryDefinition,
    override val queryInstanceId: Long,
    val singleNodeResult: QueryResult) extends AbstractRunQueryResponse("runQueryResponse", queryId, createDate, userId, groupId, requestXml, queryInstanceId) {

  override val results = Seq(singleNodeResult)
  
  def withResult(res: QueryResult): RunQueryResponse = this.copy(singleNodeResult = res)
}

object RunQueryResponse extends AbstractRunQueryResponse.Companion[RunQueryResponse]