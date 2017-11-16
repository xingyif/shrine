package net.shrine.protocol

import net.shrine.audit.NetworkQueryId

/**
 * @author Bill Simons
 * @since 4/13/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 *
 * NB: this is a case class to get a structural equality contract in hashCode and equals, mostly for testing
 *
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 * NOTE: Now that the adapter caches/stores results from the CRC, Instead of an
 * i2b2 instance id, this class now contains the Shrine-generated, network-wide
 * id of a query, which was used to obtain results previously obtained from the
 * CRC from Shrine's datastore.
 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
 */
final case class ReadInstanceResultsResponse(
    /*
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     * NOTE: Now that the adapter caches/stores results from the CRC, Instead of an
     * i2b2 instance id, this class now contains the Shrine-generated, network-wide 
     * id of a query, which is used to obtain results previously obtained from the 
     * CRC from Shrine's datastore.
     * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
     */
    override val shrineNetworkQueryId: Long,
    singleNodeResult: QueryResult) extends AbstractReadInstanceResultsResponse("readInstanceResultsResponse", shrineNetworkQueryId) {

  override def results = Seq(singleNodeResult)
}

object ReadInstanceResultsResponse extends AbstractReadInstanceResultsResponse.Companion[ReadInstanceResultsResponse] {

  def apply(
             networkQueryId: NetworkQueryId,
             resultId:Long,
             rqir:ReadQueryInstancesResponse):ReadInstanceResultsResponse = {
    val singleNodeResult:QueryResult = QueryResult(
      resultId = resultId,
      instanceId = rqir.queryInstances.head.queryInstanceId.toLong,
      resultType = None, //maybe PATIENT_COUNT_XML, probably doesn't matter
      setSize = -1L, //no result available
      startDate = Some(rqir.queryInstances.head.startDate),
      endDate = rqir.queryInstances.head.endDate,
      description = None,
      statusType = rqir.queryInstances.head.queryStatus,
      statusMessage = None
    )

    ReadInstanceResultsResponse(networkQueryId,singleNodeResult)
  }
}