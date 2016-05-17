package net.shrine.protocol

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.query.QueryDefinition

/**
 * @author clint
 * @since Nov 30, 2012
 */
final case class AggregatedRunQueryResponse(
    override val queryId: Long, //not the network query id , which doesn't actually come back anywhere!
    override val createDate: XMLGregorianCalendar,
    override val userId: String,
    override val groupId: String,
    override val requestXml: QueryDefinition,
    override val queryInstanceId: Long,
    override val results: Seq[QueryResult]) extends AbstractRunQueryResponse("aggregatedRunQueryResponse", queryId, createDate, userId, groupId, requestXml, queryInstanceId) {

  def withResults(seq: Seq[QueryResult]) = this.copy(results = seq)
  
  def resultsPartitioned = results.partition(!_.isError)
}

object AggregatedRunQueryResponse extends AbstractRunQueryResponse.Companion[AggregatedRunQueryResponse]