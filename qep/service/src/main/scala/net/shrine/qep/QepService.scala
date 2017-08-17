package net.shrine.qep

import com.typesafe.config.Config
import net.shrine.aggregation.{ReadQueryResultAggregator, ReadTranslatedQueryDefinitionAggregator}
import net.shrine.authentication.Authenticator
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.BroadcastAndAggregationService
import net.shrine.config.DurationConfigParser
import net.shrine.protocol.{BaseShrineResponse, DeleteQueryRequest, FlagQueryRequest, NodeId, ReadApprovedQueryTopicsRequest, ReadInstanceResultsRequest, ReadPreviousQueriesRequest, ReadQueryDefinitionRequest, ReadQueryInstancesRequest, ReadQueryResultRequest, ReadTranslatedQueryDefinitionRequest, RenameQueryRequest, ResultOutputType, RunQueryRequest, ShrineRequestHandler, UnFlagQueryRequest}
import net.shrine.qep.dao.AuditDao

import scala.concurrent.duration.Duration

/**
 * @author Bill Simons
 * @since 3/23/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final case class QepService(
    commonName:String,
    auditDao: AuditDao,
    authenticator: Authenticator,
    authorizationService: QueryAuthorizationService,
    includeAggregateResult: Boolean,
    broadcastAndAggregationService: BroadcastAndAggregationService,
    queryTimeout: Duration,
    breakdownTypes: Set[ResultOutputType],
    collectQepAudit:Boolean,
    nodeId:NodeId
  ) extends AbstractQepService[BaseShrineResponse] with ShrineRequestHandler {

  debug(s"ShrineService collectQepAudit is $collectQepAudit")

  override def flagQuery(request: FlagQueryRequest, shouldBroadcast: Boolean = true) = doFlagQuery(request, shouldBroadcast)
  
  override def unFlagQuery(request: UnFlagQueryRequest, shouldBroadcast: Boolean = true) = doUnFlagQuery(request, shouldBroadcast)
  
  override def readTranslatedQueryDefinition(request: ReadTranslatedQueryDefinitionRequest, shouldBroadcast: Boolean = true): BaseShrineResponse = authenticateAndThen(request) { authResult => {
    doBroadcastQuery(request, new ReadTranslatedQueryDefinitionAggregator, shouldBroadcast,authResult)
    }
  }
  
  override def runQuery(request: RunQueryRequest, shouldBroadcast: Boolean) = doRunQuery(request, shouldBroadcast)

  override def readQueryDefinition(request: ReadQueryDefinitionRequest, shouldBroadcast: Boolean) = doReadQueryDefinition(request, shouldBroadcast)

  override def readInstanceResults(request: ReadInstanceResultsRequest, shouldBroadcast: Boolean) = doReadInstanceResults(request, shouldBroadcast)

  override def readQueryInstances(request: ReadQueryInstancesRequest, shouldBroadcast: Boolean) = doReadQueryInstances(request, shouldBroadcast)

  override def readPreviousQueries(request: ReadPreviousQueriesRequest, shouldBroadcast: Boolean) = doReadPreviousQueries(request, shouldBroadcast)

  override def renameQuery(request: RenameQueryRequest, shouldBroadcast: Boolean) = doRenameQuery(request, shouldBroadcast)

  override def deleteQuery(request: DeleteQueryRequest, shouldBroadcast: Boolean) = doDeleteQuery(request, shouldBroadcast)

  override def readApprovedQueryTopics(request: ReadApprovedQueryTopicsRequest, shouldBroadcast: Boolean) = doReadApprovedQueryTopics(request, shouldBroadcast)

  override def readQueryResult(request: ReadQueryResultRequest, shouldBroadcast: Boolean) = {
    authenticateAndThen(request) { authResult => doBroadcastQuery(request, new ReadQueryResultAggregator(request.queryId, includeAggregateResult), shouldBroadcast, authResult)
    }
  }
}

object QepService {

  def apply(qepConfig:Config,
            commonName: String,
            auditDao: AuditDao,
            authenticator: Authenticator,
            authorizationService: QueryAuthorizationService,
            broadcastService: BroadcastAndAggregationService,
            breakdownTypes: Set[ResultOutputType],
            nodeId:NodeId
           ):QepService = {
    QepService(
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