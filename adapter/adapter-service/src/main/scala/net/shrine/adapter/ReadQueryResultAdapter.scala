package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.client.Poster
import net.shrine.protocol.{HiveCredentials, ReadQueryResultRequest, ReadQueryResultResponse, ResultOutputType}


/**
 * @author clint
 * @since Nov 2, 2012
 * 
 */
final class ReadQueryResultAdapter(
	poster: Poster,
	override val hiveCredentials: HiveCredentials,
	dao: AdapterDao,
	doObfuscation: Boolean,
	breakdownTypes: Set[ResultOutputType],
	collectAdapterAudit:Boolean,
	obfuscator: Obfuscator
) extends AbstractReadQueryResultAdapter[ReadQueryResultRequest, ReadQueryResultResponse](
	poster = poster,
	hiveCredentials = hiveCredentials,
	dao = dao,
	doObfuscation = doObfuscation,
	getQueryId = _.queryId,
	getProjectId = _.projectId,
	toResponse = ReadQueryResultResponse(_, _),
	breakdownTypes = breakdownTypes,
	collectAdapterAudit = collectAdapterAudit,
	obfuscator = obfuscator
)

