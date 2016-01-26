package net.shrine.qep

import net.shrine.protocol.I2b2RequestHandler
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ReadInstanceResultsRequest
import net.shrine.protocol.ReadPreviousQueriesRequest
import net.shrine.protocol.RenameQueryRequest
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.ReadPdoRequest
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.protocol.ReadQueryInstancesRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.qep.dao.AuditDao
import net.shrine.authentication.Authenticator
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.BroadcastAndAggregationService
import scala.concurrent.duration.Duration
import net.shrine.protocol.FlagQueryRequest
import net.shrine.protocol.UnFlagQueryRequest
import net.shrine.protocol.ReadResultOutputTypesRequest
import net.shrine.protocol.ResultOutputType

/**
 * @author clint
 * @since Feb 19, 2014
 */
final case class I2b2BroadcastService(
    commonName:String,
    auditDao: AuditDao,
    authenticator: Authenticator,
    authorizationService: QueryAuthorizationService,
    includeAggregateResult: Boolean,
    broadcastAndAggregationService: BroadcastAndAggregationService,
    queryTimeout: Duration,
    breakdownTypes: Set[ResultOutputType],
    collectQepAudit:Boolean) extends AbstractShrineService[ShrineResponse] with I2b2RequestHandler {

  override def readResultOutputTypes(request: ReadResultOutputTypesRequest): ShrineResponse = doReadResultOutputTypes(request)
  
  override def runQuery(request: RunQueryRequest, shouldBroadcast: Boolean): ShrineResponse = doRunQuery(request, shouldBroadcast)

  override def readQueryDefinition(request: ReadQueryDefinitionRequest, shouldBroadcast: Boolean) = doReadQueryDefinition(request, shouldBroadcast)

  override def readPdo(request: ReadPdoRequest, shouldBroadcast: Boolean) = doReadPdo(request, shouldBroadcast)

  override def readInstanceResults(request: ReadInstanceResultsRequest, shouldBroadcast: Boolean) = doReadInstanceResults(request, shouldBroadcast)

  override def readQueryInstances(request: ReadQueryInstancesRequest, shouldBroadcast: Boolean) = doReadQueryInstances(request, shouldBroadcast)

  override def readPreviousQueries(request: ReadPreviousQueriesRequest, shouldBroadcast: Boolean) = doReadPreviousQueries(request, shouldBroadcast)

  override def renameQuery(request: RenameQueryRequest, shouldBroadcast: Boolean) = doRenameQuery(request, shouldBroadcast)

  override def deleteQuery(request: DeleteQueryRequest, shouldBroadcast: Boolean) = doDeleteQuery(request, shouldBroadcast)

  override def readApprovedQueryTopics(request: ReadApprovedQueryTopicsRequest, shouldBroadcast: Boolean) = doReadApprovedQueryTopics(request, shouldBroadcast)
  
  override def flagQuery(request: FlagQueryRequest, shouldBroadcast: Boolean = true) = doFlagQuery(request, shouldBroadcast)
  
  override def unFlagQuery(request: UnFlagQueryRequest, shouldBroadcast: Boolean = true) = doUnFlagQuery(request, shouldBroadcast)
}