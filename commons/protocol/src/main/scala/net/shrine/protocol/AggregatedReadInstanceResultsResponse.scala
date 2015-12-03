package net.shrine.protocol

/**
 * @author clint
 * @date Dec 4, 2012
 */
final case class AggregatedReadInstanceResultsResponse(
    /*
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * NOTE: Now that the adapter caches/stores results from the CRC, Instead of an
     * i2b2 instance id, this class now contains the Shrine-generated, network-wide 
     * id of a query, which is used to obtain results previously obtained from the 
     * CRC from Shrine's datastore.
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    override val shrineNetworkQueryId: Long,
    override val results: Seq[QueryResult]) extends AbstractReadInstanceResultsResponse("aggregatedReadInstanceResultsResponse", shrineNetworkQueryId) {

  override type ActualResponseType = AggregatedReadInstanceResultsResponse

  override def withId(id: Long) = this.copy(shrineNetworkQueryId = id)
  
  def withResults(seq: Seq[QueryResult]) = this.copy(results = seq)
}

object AggregatedReadInstanceResultsResponse extends AbstractReadInstanceResultsResponse.Companion[AggregatedReadInstanceResultsResponse]