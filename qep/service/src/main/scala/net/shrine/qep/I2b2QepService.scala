package net.shrine.qep

import com.typesafe.config.Config
import net.shrine.authentication.Authenticator
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.BroadcastAndAggregationService
import net.shrine.config.DurationConfigParser
import net.shrine.protocol.{DeleteQueryRequest, FlagQueryRequest, I2b2RequestHandler, NodeId, ReadApprovedQueryTopicsRequest, ReadInstanceResultsRequest, ReadPreviousQueriesRequest, ReadQueryDefinitionRequest, ReadQueryInstancesRequest, ReadResultOutputTypesRequest, RenameQueryRequest, ResultOutputType, RunQueryRequest, ShrineResponse, UnFlagQueryRequest}
import net.shrine.qep.dao.AuditDao

import scala.concurrent.duration.Duration

/**
 * @author clint
 * @since Feb 19, 2014
 */
final case class I2b2QepService(
    commonName:String,
    auditDao: AuditDao,
    authenticator: Authenticator,
    authorizationService: QueryAuthorizationService,
    includeAggregateResult: Boolean,
    broadcastAndAggregationService: BroadcastAndAggregationService,
    queryTimeout: Duration,
    breakdownTypes: Set[ResultOutputType],
    collectQepAudit:Boolean,
    nodeId: NodeId
  ) extends AbstractQepService[ShrineResponse] with I2b2RequestHandler {

  override def readResultOutputTypes(request: ReadResultOutputTypesRequest): ShrineResponse = doReadResultOutputTypes(request)
  
  override def runQuery(request: RunQueryRequest, shouldBroadcast: Boolean): ShrineResponse = doRunQuery(request, shouldBroadcast)

  override def readQueryDefinition(request: ReadQueryDefinitionRequest, shouldBroadcast: Boolean) = doReadQueryDefinition(request, shouldBroadcast)

  override def readInstanceResults(request: ReadInstanceResultsRequest, shouldBroadcast: Boolean) = doReadInstanceResults(request, shouldBroadcast)

  override def readQueryInstances(request: ReadQueryInstancesRequest, shouldBroadcast: Boolean) = doReadQueryInstances(request, shouldBroadcast)

  override def readPreviousQueries(request: ReadPreviousQueriesRequest, shouldBroadcast: Boolean) = doReadPreviousQueries(request, shouldBroadcast)

  override def renameQuery(request: RenameQueryRequest, shouldBroadcast: Boolean) = doRenameQuery(request, shouldBroadcast)

  override def deleteQuery(request: DeleteQueryRequest, shouldBroadcast: Boolean) = doDeleteQuery(request, shouldBroadcast)

  override def readApprovedQueryTopics(request: ReadApprovedQueryTopicsRequest, shouldBroadcast: Boolean) = doReadApprovedQueryTopics(request, shouldBroadcast)
  
  override def flagQuery(request: FlagQueryRequest, shouldBroadcast: Boolean = true) = doFlagQuery(request, shouldBroadcast)
  
  override def unFlagQuery(request: UnFlagQueryRequest, shouldBroadcast: Boolean = true) = doUnFlagQuery(request, shouldBroadcast)
}

object I2b2QepService {

  def apply(qepConfig: Config,
            commonName: String,
            auditDao: AuditDao,
            authenticator: Authenticator,
            authorizationService: QueryAuthorizationService,
            broadcastService: BroadcastAndAggregationService,
            breakdownTypes: Set[ResultOutputType],
            nodeId: NodeId
           ): I2b2QepService = {
    I2b2QepService(
      commonName,
      auditDao,
      authenticator,
      authorizationService,
      qepConfig.getBoolean("includeAggregateResults"),
      broadcastService,
      DurationConfigParser(qepConfig.getConfig("maxQueryWaitTime")),
      breakdownTypes,
      qepConfig.getBoolean("audit.collectQepAudit"),
      nodeId
    )
  }
}