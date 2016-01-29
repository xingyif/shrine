package net.shrine.qep

import net.shrine.log.Loggable
import net.shrine.qep.audit.QepAuditDb
import net.shrine.qep.dao.AuditDao
import net.shrine.authentication.Authenticator
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.broadcaster.BroadcastAndAggregationService
import net.shrine.qep.queries.QepQueryDb
import scala.concurrent.duration.Duration
import net.shrine.util.XmlDateHelper
import scala.concurrent.Future
import scala.concurrent.Await
import net.shrine.protocol.{ReadPreviousQueriesResponse, RunQueryRequest, BaseShrineRequest, AuthenticationInfo, Credential, BaseShrineResponse, ReadQueryInstancesRequest, QueryInstance, ReadQueryInstancesResponse, ReadQueryDefinitionRequest, DeleteQueryRequest, ReadApprovedQueryTopicsRequest, ReadInstanceResultsRequest, ReadPreviousQueriesRequest, RenameQueryRequest, ReadPdoRequest, FlagQueryRequest, UnFlagQueryRequest, ReadResultOutputTypesRequest, ReadResultOutputTypesResponse, ResultOutputType}
import net.shrine.authorization.AuthorizationResult.{Authorized, NotAuthorized}
import net.shrine.authentication.AuthenticationResult
import net.shrine.authentication.NotAuthenticatedException
import net.shrine.aggregation.RunQueryAggregator
import net.shrine.aggregation.Aggregators
import net.shrine.aggregation.Aggregator
import net.shrine.aggregation.ReadQueryDefinitionAggregator
import net.shrine.aggregation.DeleteQueryAggregator
import net.shrine.aggregation.ReadPdoResponseAggregator
import net.shrine.aggregation.RenameQueryAggregator
import net.shrine.aggregation.ReadInstanceResultsAggregator
import net.shrine.aggregation.FlagQueryAggregator
import net.shrine.aggregation.UnFlagQueryAggregator

/**
 * @author clint
 * @since Feb 19, 2014
 */

trait AbstractQepService[BaseResp <: BaseShrineResponse] extends Loggable {
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

    authenticateAndThen(request) { authResult =>
      val resultOutputTypes = ResultOutputType.nonErrorTypes ++ breakdownTypes

      //TODO: XXX: HACK: Would like to remove the cast
      ReadResultOutputTypesResponse(resultOutputTypes).asInstanceOf[BaseResp]
    }
  }
  
  protected def doFlagQuery(request: FlagQueryRequest, shouldBroadcast: Boolean = true): BaseResp = {
    QepQueryDb.db.insertQepQueryFlag(request)
    doBroadcastQuery(request, new FlagQueryAggregator, shouldBroadcast)
  }
  
  protected def doUnFlagQuery(request: UnFlagQueryRequest, shouldBroadcast: Boolean = true): BaseResp = {
    QepQueryDb.db.insertQepQueryFlag(request)
    doBroadcastQuery(request, new UnFlagQueryAggregator, shouldBroadcast)
  }
  
  protected def doRunQuery(request: RunQueryRequest, shouldBroadcast: Boolean): BaseResp = {
    info(s"doRunQuery($request,$shouldBroadcast) with $runQueryAggregatorFor")

    //store the query in the qep's database

    doBroadcastQuery(request, runQueryAggregatorFor(request), shouldBroadcast)
  }

  protected def doReadQueryDefinition(request: ReadQueryDefinitionRequest, shouldBroadcast: Boolean): BaseResp = {
    info(s"doReadQueryDefinition($request,$shouldBroadcast)")

    doBroadcastQuery(request, new ReadQueryDefinitionAggregator, shouldBroadcast)
  }

  protected def doReadPdo(request: ReadPdoRequest, shouldBroadcast: Boolean): BaseResp = {
    info(s"doReadPdo($request,$shouldBroadcast)")
    doBroadcastQuery(request, new ReadPdoResponseAggregator, shouldBroadcast)
  }

  protected def doReadInstanceResults(request: ReadInstanceResultsRequest, shouldBroadcast: Boolean): BaseResp = {
    info(s"doReadInstanceResults($request,$shouldBroadcast)")

    //todo try reading directly from the QEP database code here

    doBroadcastQuery(request, new ReadInstanceResultsAggregator(request.shrineNetworkQueryId, false), shouldBroadcast)
  }

  protected def doReadQueryInstances(request: ReadQueryInstancesRequest, shouldBroadcast: Boolean): BaseResp = {
    info(s"doReadQueryInstances($request,$shouldBroadcast)")
    authenticateAndThen(request) { authResult =>
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

  protected def doReadPreviousQueries(request: ReadPreviousQueriesRequest, shouldBroadcast: Boolean): ReadPreviousQueriesResponse = {
    info(s"doReadPreviousQueries($request,$shouldBroadcast)")

    //check results. If any results are in one of many pending states, go ahead and request them. (Maybe go async)

    //pull queries from the local database.
    QepQueryDb.db.selectPreviousQueries(request)
  }

  protected def doRenameQuery(request: RenameQueryRequest, shouldBroadcast: Boolean): BaseResp = {
    info(s"doRenameQuery($request,$shouldBroadcast)")
    doBroadcastQuery(request, new RenameQueryAggregator, shouldBroadcast)
  }

  protected def doDeleteQuery(request: DeleteQueryRequest, shouldBroadcast: Boolean): BaseResp = {
    info(s"doDeleteQuery($request,$shouldBroadcast)")
    doBroadcastQuery(request, new DeleteQueryAggregator, shouldBroadcast)
  }

  protected def doReadApprovedQueryTopics(request: ReadApprovedQueryTopicsRequest, shouldBroadcast: Boolean): BaseResp = authenticateAndThen(request) { _ =>
    info(s"doReadApprovedQueryTopics($request,$shouldBroadcast)")
    //TODO: XXX: HACK: Would like to remove the cast
    authorizationService.readApprovedEntries(request) match {
      case Left(errorResponse) => errorResponse.asInstanceOf[BaseResp]
      case Right(validResponse) => validResponse.asInstanceOf[BaseResp]
    }
  }

  import broadcastAndAggregationService.sendAndAggregate

  protected def doBroadcastQuery(request: BaseShrineRequest, aggregator: Aggregator, shouldBroadcast: Boolean): BaseResp = {

    authenticateAndThen(request) { authResult =>

      debug(s"doBroadcastQuery($request) authResult is $authResult")
      //NB: Use credentials obtained from Authenticator (oddly, we authenticate with one set of credentials and are "logged in" under (possibly!) another
      //When making BroadcastMessages
      val networkAuthn = AuthenticationInfo(authResult.domain, authResult.username, Credential("", isToken = false))

      //NB: Only audit RunQueryRequests
      request match {
        case runQueryRequest: RunQueryRequest =>
          // inject modified, authorized runQueryRequest
          auditAuthorizeAndThen(runQueryRequest) { authorizedRequest =>
            debug(s"doBroadcastQuery authorizedRequest is $authorizedRequest")

            // tuck the ACT audit metrics data into a database here
            if (collectQepAudit) QepAuditDb.db.insertQepQuery(authorizedRequest,commonName)
            QepQueryDb.db.insertQepQuery(authorizedRequest)

            doSynchronousQuery(networkAuthn,authorizedRequest,aggregator,shouldBroadcast)
          }
        case _ => doSynchronousQuery(networkAuthn,request,aggregator,shouldBroadcast)
      }
    }
  }

  private def doSynchronousQuery(networkAuthn: AuthenticationInfo,request: BaseShrineRequest, aggregator: Aggregator, shouldBroadcast: Boolean) = {
    info(s"doSynchronousQuery($request) started")
    val response = waitFor(sendAndAggregate(networkAuthn, request, aggregator, shouldBroadcast)).asInstanceOf[BaseResp]
    info(s"doSynchronousQuery($request) completed with response $response")
    response
  }

  private[qep] val runQueryAggregatorFor: RunQueryRequest => RunQueryAggregator = Aggregators.forRunQueryRequest(includeAggregateResult)

  protected def waitFor[R](futureResponse: Future[R]): R = {
    XmlDateHelper.time("Waiting for aggregated results")(debug(_)) {
      Await.result(futureResponse, queryTimeout)
    }
  }

  private[qep] def auditAuthorizeAndThen[T](request: RunQueryRequest)(body: (RunQueryRequest => T)): T = {
    auditTransactionally(request) {

      debug(s"auditAuthorizeAndThen($request) with $authorizationService")

      val authorizedRequest = authorizationService.authorizeRunQueryRequest(request) match {
        case na: NotAuthorized => throw na.toException
        case authorized: Authorized => request.copy(topicName = authorized.topicIdAndName.map(x => x._2))
      }

      body(authorizedRequest)
    }
  }

  private[qep] def auditTransactionally[T](request: RunQueryRequest)(body: => T): T = {
    try { body } finally {
      auditDao.addAuditEntry(
        request.projectId,
        request.authn.domain,
        request.authn.username,
        request.queryDefinition.toI2b2String, //TODO: Use i2b2 format Still?
        request.topicId)
    }
  }

  import AuthenticationResult._
  
  private[qep] def authenticateAndThen[T](request: BaseShrineRequest)(f: Authenticated => T): T = {
    val AuthenticationInfo(domain, username, _) = request.authn

    val authResult = authenticator.authenticate(request.authn)

    authResult match {
      case a: Authenticated => f(a)
      case NotAuthenticated(_, _, reason) => throw new NotAuthenticatedException(s"User $domain:$username could not be authenticated: $reason")
    }
  }
}