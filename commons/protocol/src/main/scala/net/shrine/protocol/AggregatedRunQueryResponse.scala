package net.shrine.protocol

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.query.QueryDefinition

/**
 * @author clint
 * @since Nov 30, 2012
 */
//todo find where the QEP creates one of these and replace it with a different status type? - tracking SHRINE-2140
final case class AggregatedRunQueryResponse(
    override val queryId: Long, //this is the network id! - tracking SHRINE-2140
    override val createDate: XMLGregorianCalendar,
    override val userId: String,
    override val groupId: String,
    override val requestXml: QueryDefinition,
    override val queryInstanceId: Long,
    override val results: Seq[QueryResult],
    override val statusTypeName:String = "DONE"
                                           ) extends AbstractRunQueryResponse(
                                                        "aggregatedRunQueryResponse",
                                                        queryId,
                                                        createDate,
                                                        userId,
                                                        groupId,
                                                        requestXml,
                                                        queryInstanceId,
                                                        statusTypeName
                                                      ) {

  def withResults(seq: Seq[QueryResult]) = this.copy(results = seq)
  
  def resultsPartitioned = results.partition(!_.isError)
} //todo possible to add a query status with a different default status here, passed to the parent - tracking SHRINE-2140

object AggregatedRunQueryResponse extends AbstractRunQueryResponse.Companion[AggregatedRunQueryResponse]