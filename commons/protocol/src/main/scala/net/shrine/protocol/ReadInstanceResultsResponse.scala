package net.shrine.protocol

import xml.NodeSeq
import net.shrine.util.XmlUtil

/**
 * @author Bill Simons
 * @date 4/13/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
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
    val singleNodeResult: QueryResult) extends AbstractReadInstanceResultsResponse("readInstanceResultsResponse", shrineNetworkQueryId) {
  
  override type ActualResponseType = ReadInstanceResultsResponse

  override def withId(id: Long) = this.copy(shrineNetworkQueryId = id)
  
  def withQueryResult(qr: QueryResult) = this.copy(singleNodeResult = qr)
  
  override def results = Seq(singleNodeResult)
}

object ReadInstanceResultsResponse extends AbstractReadInstanceResultsResponse.Companion[ReadInstanceResultsResponse]