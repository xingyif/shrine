package net.shrine.adapter

import xml.NodeSeq
import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.{HiveCredentials, ReadInstanceResultsResponse, ReadInstanceResultsRequest, BroadcastMessage, ShrineResponse, ErrorResponse, ResultOutputType}
import net.shrine.serialization.XmlMarshaller
import net.shrine.client.HttpClient
import net.shrine.client.Poster

/**
 * @author Bill Simons
 * @since 4/14/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class ReadInstanceResultsAdapter(
	poster: Poster,
	override val hiveCredentials: HiveCredentials,
	dao: AdapterDao,
	doObfuscation: Boolean,
	breakdownTypes: Set[ResultOutputType],
	collectAdapterAudit:Boolean
) extends AbstractReadQueryResultAdapter[ReadInstanceResultsRequest, ReadInstanceResultsResponse](
    	    poster,
    	    hiveCredentials,
    		dao,
    		doObfuscation,
    		_.shrineNetworkQueryId,
    		_.projectId,
    		ReadInstanceResultsResponse(_, _),
    		breakdownTypes,
				collectAdapterAudit
)

