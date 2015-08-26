package net.shrine.adapter

import net.shrine.adapter.dao.AdapterDao
import net.shrine.protocol.{HiveCredentials, BroadcastMessage, ErrorResponse, ReadQueryResultRequest, ReadQueryResultResponse, ResultOutputType}
import net.shrine.serialization.XmlMarshaller
import net.shrine.client.HttpClient
import net.shrine.client.Poster


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
	collectAdapterAudit:Boolean
) extends AbstractReadQueryResultAdapter[ReadQueryResultRequest, ReadQueryResultResponse](
	poster,
	hiveCredentials,
	dao,
	doObfuscation,
	_.queryId,
	_.projectId,
	ReadQueryResultResponse(_, _),
	breakdownTypes,
	collectAdapterAudit
)

