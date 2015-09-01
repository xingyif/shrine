package net.shrine.service

import net.shrine.log.Loggable
import net.shrine.service.audit.QepAuditDb
import net.shrine.service.dao.AuditDao
import net.shrine.authentication.Authenticator
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.BroadcastAndAggregationService
import scala.concurrent.duration.Duration
import net.shrine.util.XmlDateHelper
import scala.concurrent.Future
import scala.concurrent.Await
import net.shrine.protocol.RunQueryRequest
import net.shrine.authorization.AuthorizationResult.NotAuthorized
import net.shrine.protocol.BaseShrineRequest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.authentication.AuthenticationResult
import net.shrine.authentication.NotAuthenticatedException
import net.shrine.aggregation.RunQueryAggregator
import net.shrine.aggregation.Aggregators
import net.shrine.protocol.BaseShrineResponse
import net.shrine.aggregation.Aggregator
import net.shrine.protocol.ReadQueryInstancesRequest
import net.shrine.protocol.QueryInstance
import net.shrine.protocol.ReadQueryInstancesResponse
import net.shrine.protocol.ReadQueryDefinitionRequest
import net.shrine.aggregation.ReadQueryDefinitionAggregator
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.aggregation.ReadPreviousQueriesAggregator
import net.shrine.aggregation.DeleteQueryAggregator
import net.shrine.aggregation.ReadPdoResponseAggregator
import net.shrine.aggregation.RenameQueryAggregator
import net.shrine.aggregation.ReadInstanceResultsAggregator
import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ReadInstanceResultsRequest
import net.shrine.protocol.ReadPreviousQueriesRequest
import net.shrine.protocol.RenameQueryRequest
import net.shrine.protocol.ReadPdoRequest
import net.shrine.protocol.FlagQueryRequest
import net.shrine.aggregation.FlagQueryAggregator
import net.shrine.protocol.UnFlagQueryRequest
import net.shrine.aggregation.UnFlagQueryAggregator
import net.shrine.protocol.ReadResultOutputTypesRequest
import net.shrine.protocol.ReadResultOutputTypesResponse
import net.shrine.protocol.ResultOutputType

/**
 * @author clint
 * @since Feb 19, 2014
 */

//todo rename? This is the heart of the QEP.
trait AbstractShrineService[BaseResp <: BaseShrineResponse] extends Loggable {
  val commonName:String
  val auditDao: AuditDao
  val authenticator: Authenticator
  val authorizationService: QueryAuthorizationService
  val includeAggregateResult: Boolean
  val broadcastAndAggregationService: BroadcastAndAggregationService
  val queryTimeout: Duration
  val breakdownTypes: Set[ResultOutputType]
  val collectQepAudit:Boolean

  protected def doReadResultOutputTypes(request: ReadResultOutputTypesRequest): BaseResp = {
    info(s"doReadResultOutputTypes($request)")

    afterAuthenticating(request) { authResult =>
      val resultOutputTypes = ResultOutputType.nonErrorTypes ++ breakdownTypes

      //TODO: XXX: HACK: Would like to remove the cast
      ReadResultOutputTypesResponse(resultOutputTypes).asInstanceOf[BaseResp]
    }
  }
  
  protected def doFlagQuery(request: FlagQueryRequest, shouldBroadcast: Boolean = true): BaseResp = {
    doBroadcastQuery(request, new FlagQueryAggregator, shouldBroadcast)
  }
  
  protected def doUnFlagQuery(request: UnFlagQueryRequest, shouldBroadcast: Boolean = true): BaseResp = {
    doBroadcastQuery(request, new UnFlagQueryAggregator, shouldBroadcast)
  }
  
  protected def doRunQuery(request: RunQueryRequest, shouldBroadcast: Boolean): BaseResp = {
    info(s"doRunQuery($request,$shouldBroadcast) with $runQueryAggregatorFor")

    val result = doBroadcastQuery(request, runQueryAggregatorFor(request), shouldBroadcast)

    debug(s"collectQepAudit is $collectQepAudit")

    // tuck the ACT audit metrics data into a database here
    //todo network id is -1 !
    if (collectQepAudit) QepAuditDb.db.insertQepQuery(request,commonName)

    result
  }

  protected def doReadQueryDefinition(request: ReadQueryDefinitionRequest, shouldBroadcast: Boolean): BaseResp = {
    doBroadcastQuery(request, new ReadQueryDefinitionAggregator, shouldBroadcast)
  }

  protected def doReadPdo(request: ReadPdoRequest, shouldBroadcast: Boolean): BaseResp = {
    doBroadcastQuery(request, new ReadPdoResponseAggregator, shouldBroadcast)
  }

  protected def doReadInstanceResults(request: ReadInstanceResultsRequest, shouldBroadcast: Boolean): BaseResp = {
    doBroadcastQuery(request, new ReadInstanceResultsAggregator(request.shrineNetworkQueryId, false), shouldBroadcast)
  }

  protected def doReadQueryInstances(request: ReadQueryInstancesRequest, shouldBroadcast: Boolean): BaseResp = {
    info(s"doReadQueryInstances($request)")
    afterAuthenticating(request) { authResult =>
      val now = XmlDateHelper.now
      val networkQueryId = request.queryId
      val username = request.authn.username
      val groupId = request.projectId

      //NB: Return a dummy response, with a dummy QueryInstance containing the network (Shrine) id of the query we'd like
      //to get "instances" for.  This allows the legacy web client to formulate a request for query results that Shrine
      //can understand, while meeting the conversational requirements of the legacy web client.
      val instance = QueryInstance(networkQueryId.toString, networkQueryId.toString, username, groupId, now, now)

      //TODO: XXX: HACK: Would like to remove the cast
      //NB: Munge in username from authentication result
      ReadQueryInstancesResponse(networkQueryId, authResult.username, groupId, Seq(instance)).asInstanceOf[BaseResp]
    }
  }

  protected def doReadPreviousQueries(request: ReadPreviousQueriesRequest, shouldBroadcast: Boolean): BaseResp = {
    doBroadcastQuery(request, new ReadPreviousQueriesAggregator, shouldBroadcast)
  }

  protected def doRenameQuery(request: RenameQueryRequest, shouldBroadcast: Boolean): BaseResp = {
    doBroadcastQuery(request, new RenameQueryAggregator, shouldBroadcast)
  }

  protected def doDeleteQuery(request: DeleteQueryRequest, shouldBroadcast: Boolean): BaseResp = {
    doBroadcastQuery(request, new DeleteQueryAggregator, shouldBroadcast)
  }

  protected def doReadApprovedQueryTopics(request: ReadApprovedQueryTopicsRequest, shouldBroadcast: Boolean): BaseResp = afterAuthenticating(request) { _ =>
    info(s"doReadApprovedQueryTopics($request)")
    //TODO: Is authenticating necessary?
    //TODO: XXX: HACK: Would like to remove the cast
    authorizationService.readApprovedEntries(request) match {
      case Left(errorResponse) => errorResponse.asInstanceOf[BaseResp]
      case Right(validResponse) => validResponse.asInstanceOf[BaseResp]
    }
  }

  import broadcastAndAggregationService.sendAndAggregate

  protected def doBroadcastQuery(request: BaseShrineRequest, aggregator: Aggregator, shouldBroadcast: Boolean): BaseResp = {

    info(s"doBroadcastQuery($request)")

    //TODO: XXX: HACK: Would like to remove the cast
    def doSynchronousQuery(networkAuthn: AuthenticationInfo) = waitFor(sendAndAggregate(networkAuthn, request, aggregator, shouldBroadcast)).asInstanceOf[BaseResp]

    afterAuthenticating(request) { authResult =>

      debug(s"doBroadcastQuery($request) authResult is $authResult")
      //NB: Use credentials obtained from Authenticator (oddly, we authenticate with one set of credentials and are "logged in" under (possibly!) another
      //When making BroadcastMessages
      val networkAuthn = AuthenticationInfo(authResult.domain, authResult.username, Credential("", isToken = false))

      //NB: Only audit RunQueryRequests
      request match {
        case runQueryRequest: RunQueryRequest =>
          afterAuditingAndAuthorizing(runQueryRequest) (doSynchronousQuery(networkAuthn))
        case _ => doSynchronousQuery(networkAuthn)
      }
    }
  }

  private[service] val runQueryAggregatorFor: RunQueryRequest => RunQueryAggregator = Aggregators.forRunQueryRequest(includeAggregateResult)

  protected def waitFor[R](futureResponse: Future[R]): R = {
    XmlDateHelper.time("Waiting for aggregated results")(debug(_)) {
      Await.result(futureResponse, queryTimeout)
    }
  }

  private[service] def afterAuditingAndAuthorizing[T](request: RunQueryRequest)(body: => T): T = {
    auditTransactionally(request) {

      debug(s"afterAuditingAndAuthorizing($request) with $authorizationService")

      authorizationService.authorizeRunQueryRequest(request) match {
        case na: NotAuthorized => throw na.toException
        case _ => ()
      }

      body
    }
  }

  private[service] def auditTransactionally[T](request: RunQueryRequest)(body: => T): T = {
    try { body } finally {
      auditDao.addAuditEntry(
        request.projectId,
        request.authn.domain,
        request.authn.username,
        request.queryDefinition.toI2b2String, //TODO: Use i2b2 format Still?
        request.topicIdAndName.map(_._1)) //todo topic name, too?
    }
  }

  import AuthenticationResult._
  
  private[service] def afterAuthenticating[T](request: BaseShrineRequest)(f: Authenticated => T): T = {
    val AuthenticationInfo(domain, username, _) = request.authn

    val authResult = authenticator.authenticate(request.authn)

    authResult match {
      case a: Authenticated => f(a)
      case NotAuthenticated(_, _, reason) => throw new NotAuthenticatedException(s"User $domain:$username could not be authenticated: $reason")
    }
  }
}