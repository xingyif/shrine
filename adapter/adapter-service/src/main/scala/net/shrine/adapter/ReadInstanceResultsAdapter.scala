package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.client.Poster
import net.shrine.protocol.{HiveCredentials, ReadInstanceResultsRequest, ReadInstanceResultsResponse, ResultOutputType}

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
	collectAdapterAudit:Boolean,
	obfuscator: Obfuscator
) extends AbstractReadQueryResultAdapter[ReadInstanceResultsRequest, ReadInstanceResultsResponse](
		poster = poster,
		hiveCredentials = hiveCredentials,
		dao = dao,
		doObfuscation = doObfuscation,
		getQueryId = _.shrineNetworkQueryId,
		getProjectId = _.projectId,
		toResponse = ReadInstanceResultsResponse(_, _),
		breakdownTypes = breakdownTypes,
		collectAdapterAudit = collectAdapterAudit,
	  obfuscator = obfuscator
)

