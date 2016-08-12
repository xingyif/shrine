package net.shrine.adapter

import net.shrine.adapter.audit.AdapterAuditDb

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.xml.NodeSeq
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.translators.QueryDefinitionTranslator
import net.shrine.protocol.{AuthenticationInfo, BroadcastMessage, Credential, ErrorFromCrcException, ErrorResponse, HiveCredentials, I2b2ResultEnvelope, MissingCrCXmlResultException, QueryResult, RawCrcRunQueryResponse, ReadResultRequest, ReadResultResponse, ResultOutputType, RunQueryRequest, RunQueryResponse, ShrineResponse}
import net.shrine.client.Poster
import net.shrine.problem.{AbstractProblem, LoggingProblemHandler, Problem, ProblemNotYetEncoded, ProblemSources}

import scala.util.control.NonFatal
import net.shrine.util.XmlDateHelper

import scala.xml.XML

/**
 * @author Bill Simons
 * @author clint
 * @since 4/15/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final case class RunQueryAdapter(
  poster: Poster,
  dao: AdapterDao,
  override val hiveCredentials: HiveCredentials,
  conceptTranslator: QueryDefinitionTranslator,
  adapterLockoutAttemptsThreshold: Int,
  doObfuscation: Boolean,
  runQueriesImmediately: Boolean,
  breakdownTypes: Set[ResultOutputType],
  collectAdapterAudit:Boolean
) extends CrcAdapter[RunQueryRequest, RunQueryResponse](poster, hiveCredentials) {

  logStartup()

  import RunQueryAdapter._

  override protected[adapter] def parseShrineResponse(xml: NodeSeq) = RawCrcRunQueryResponse.fromI2b2(breakdownTypes)(xml).get //TODO: Avoid .get call

  override protected[adapter] def translateNetworkToLocal(request: RunQueryRequest): RunQueryRequest = {
    try { request.mapQueryDefinition(conceptTranslator.translate) }
    catch {
      case NonFatal(e) => throw new AdapterMappingException(request,s"Error mapping query terms from network to local forms.", e)
    }
  }

  override protected[adapter] def processRequest(message: BroadcastMessage): ShrineResponse = {

    if (collectAdapterAudit) AdapterAuditDb.db.insertQueryReceived(message)

    if (isLockedOut(message.networkAuthn)) {
      throw new AdapterLockoutException(message.networkAuthn,poster.url)
    }

    val runQueryReq = message.request.asInstanceOf[RunQueryRequest]

    //We need to use the network identity from the BroadcastMessage, since that will have the network username 
    //(ie, ecommons) of the querying user. Using the AuthenticationInfo from the incoming request breaks the fetching
    //of previous queries on deployed systems where the credentials in the identity param to this method and the authn
    //field of the incoming request are different, like the HMS Shrine deployment.
    //NB: Credential field is wiped out to preserve old behavior -Clint 14 Nov, 2013
    val authnToUse = message.networkAuthn.copy(credential = Credential("", isToken = false))

    if (!runQueriesImmediately) {
      debug(s"Queueing query from user ${message.networkAuthn.domain}:${message.networkAuthn.username}")

      storeQuery(authnToUse, message, runQueryReq)

    } else {
      debug(s"Performing query from user ${message.networkAuthn.domain}:${message.networkAuthn.username}")

      val result: ShrineResponse = runQuery(authnToUse, message.copy(request = runQueryReq.withAuthn(authnToUse)), runQueryReq.withAuthn(authnToUse))
      if (collectAdapterAudit) AdapterAuditDb.db.insertResultSent(runQueryReq.networkQueryId,result)

      result
    }
  }

  private def storeQuery(authnToUse: AuthenticationInfo, message: BroadcastMessage, request: RunQueryRequest): RunQueryResponse = {
    //Use dummy ids for what we would have received from the CRC
    val masterId: Long = -1L
    val queryInstanceId: Long = -1L
    val resultId: Long = -1L

    //TODO: is this right?? Or maybe it's project id?
    val groupId = authnToUse.domain

    val invalidSetSize = -1L
    val now = XmlDateHelper.now
    val queryResult = QueryResult(resultId, queryInstanceId, Some(ResultOutputType.PATIENT_COUNT_XML), invalidSetSize, Some(now), Some(now), Some("Query enqueued for later processing"), QueryResult.StatusType.Held, Some("Query enqueued for later processing"))

    dao.inTransaction {
      val insertedQueryId = dao.insertQuery(masterId.toString, request.networkQueryId, authnToUse, request.queryDefinition, isFlagged = false, hasBeenRun = false, flagMessage = None)

      val insertedQueryResultIds = dao.insertQueryResults(insertedQueryId, Seq(queryResult))

      //NB: We need to insert dummy QueryResult and Count records so that calls to StoredQueries.retrieve() in 
      //AbstractReadQueryResultAdapter, called when retrieving results for previously-queued-or-incomplete 
      //queries, will work.
      val countQueryResultId = insertedQueryResultIds(ResultOutputType.PATIENT_COUNT_XML).head

      dao.insertCountResult(countQueryResultId, -1L, -1L)
    }

    RunQueryResponse(masterId, XmlDateHelper.now, authnToUse.username, groupId, request.queryDefinition, queryInstanceId, queryResult)
  }

  private def runQuery(authnToUse: AuthenticationInfo, message: BroadcastMessage, request: RunQueryRequest): ShrineResponse = {
    if (collectAdapterAudit) AdapterAuditDb.db.insertExecutionStarted(request)

    //NB: Pass through ErrorResponses received from the CRC.
    //See: https://open.med.harvard.edu/jira/browse/SHRINE-794
    val result = super.processRequest(message) match {
      case e: ErrorResponse => e
      case rawRunQueryResponse: RawCrcRunQueryResponse => processRawCrcRunQueryResponse(authnToUse, request, rawRunQueryResponse)
    }
    if (collectAdapterAudit) AdapterAuditDb.db.insertExecutionCompletedShrineResponse(request,result)

    result
  }

  private[adapter] def processRawCrcRunQueryResponse(authnToUse: AuthenticationInfo, request: RunQueryRequest, rawRunQueryResponse: RawCrcRunQueryResponse): RunQueryResponse = {
    def isBreakdown(result: QueryResult) = result.resultType.exists(_.isBreakdown)

    val originalResults: Seq[QueryResult] = rawRunQueryResponse.results

    val (originalBreakdownResults, originalNonBreakDownResults): (Seq[QueryResult],Seq[QueryResult]) = originalResults.partition(isBreakdown)

    val originalBreakdownCountAttempts: Seq[(QueryResult, Try[QueryResult])] = attemptToRetrieveBreakdowns(request, originalBreakdownResults)
    
    val (successfulBreakdownCountAttempts, failedBreakdownCountAttempts) = originalBreakdownCountAttempts.partition { case (_, t) => t.isSuccess }

    val failedBreakdownCountAttemptsWithProblems = failedBreakdownCountAttempts.map { attempt =>
      val originalResult: QueryResult = attempt._1
      val queryResult:QueryResult = if (originalResult.problemDigest.isDefined) originalResult
      else {
        attempt._2 match {
          case Success(_) => originalResult
          case Failure(x) => //noinspection RedundantBlock
          {
            val problem:Problem = x match {
              case e: ErrorFromCrcException => ErrorFromCrcBreakdown(e)
              case e: MissingCrCXmlResultException => CannotInterpretCrcBreakdownXml(e)
              case NonFatal(e) => {
                val summary = s"Unexpected exception while interpreting breakdown response"
                ProblemNotYetEncoded(summary, e)
              }
            }
            LoggingProblemHandler.handleProblem(problem)
            originalResult.copy(problemDigest = Some(problem.toDigest))
          }
        }
      }
      (queryResult,attempt._2)
    }

    logBreakdownFailures(rawRunQueryResponse, failedBreakdownCountAttemptsWithProblems)

    val originalMergedBreakdowns: Map[ResultOutputType, I2b2ResultEnvelope] = {
      val withBreakdownCounts = successfulBreakdownCountAttempts.collect { case (_, Success(queryResultWithBreakdowns)) => queryResultWithBreakdowns }

      withBreakdownCounts.map(_.breakdowns).fold(Map.empty)(_ ++ _)
    }

    val obfuscatedQueryResults = originalResults.map(Obfuscator.obfuscate)

    val obfuscatedNonBreakdownQueryResults = obfuscatedQueryResults.filterNot(isBreakdown)

    val obfuscatedMergedBreakdowns = obfuscateBreakdowns(originalMergedBreakdowns)

    val failedBreakdownTypes = failedBreakdownCountAttemptsWithProblems.flatMap { case (qr, _) => qr.resultType }

    dao.storeResults(
      authn = authnToUse,
      masterId = rawRunQueryResponse.queryId.toString,
      networkQueryId = request.networkQueryId,
      queryDefinition = request.queryDefinition,
      rawQueryResults = originalResults,
      obfuscatedQueryResults = obfuscatedQueryResults,
      failedBreakdownTypes = failedBreakdownTypes,
      mergedBreakdowns = originalMergedBreakdowns,
      obfuscatedBreakdowns = obfuscatedMergedBreakdowns)

    // at this point the queryResult could be a mix of successes and failures.
    // SHRINE reports only the successes. See SHRINE-1567 for details
    val queryResults: Seq[QueryResult] = if (doObfuscation) obfuscatedNonBreakdownQueryResults else originalNonBreakDownResults

    val breakdownsToReturn: Map[ResultOutputType, I2b2ResultEnvelope] = if (doObfuscation) obfuscatedMergedBreakdowns else originalMergedBreakdowns

    //TODO: Will fail in the case of NO non-breakdown QueryResults.  Can this ever happen, and is it worth protecting against here?
    //can failedBreakdownCountAttempts be mixed back in here?
    val resultWithBreakdowns: QueryResult = queryResults.head.withBreakdowns(breakdownsToReturn)

    if(debugEnabled) {
      def justBreakdowns(breakdowns: Map[ResultOutputType, I2b2ResultEnvelope]) = breakdowns.mapValues(_.data)

      val obfuscationMessage = s"obfuscation is ${if(doObfuscation) "ON" else "OFF"}"

      debug(s"Returning QueryResult with count ${resultWithBreakdowns.setSize} (original count: ${originalNonBreakDownResults.headOption.map(_.setSize)} ; $obfuscationMessage)")
      debug(s"Returning QueryResult with breakdowns ${justBreakdowns(resultWithBreakdowns.breakdowns)} (original breakdowns: ${justBreakdowns(originalMergedBreakdowns)} ; $obfuscationMessage)")
      debug(s"Full QueryResult: $resultWithBreakdowns")
    }

    //if any results had problems, this commented out code can turn it into an error QueryResult
    //See SHRINE-1619
    //val problem: Option[ProblemDigest] = failedBreakdownCountAttemptsWithProblems.headOption.flatMap(x => x._1.problemDigest)
    //val queryResult = problem.fold(resultWithBreakdowns)(pd => QueryResult.errorResult(Some(pd.description),"Error with CRC",pd))

    rawRunQueryResponse.toRunQueryResponse.withResult(resultWithBreakdowns)
  }

  private def getResultFromCrc(parentRequest: RunQueryRequest, networkResultId: Long): Try[ReadResultResponse] = {
    def readResultRequest(runQueryReq: RunQueryRequest, networkResultId: Long) = ReadResultRequest(hiveCredentials.projectId, runQueryReq.waitTime, hiveCredentials.toAuthenticationInfo, networkResultId.toString)

    Try(XML.loadString(callCrc(readResultRequest(parentRequest, networkResultId)))).flatMap(ReadResultResponse.fromI2b2(breakdownTypes))
  }

  private[adapter] def attemptToRetrieveCount(runQueryReq: RunQueryRequest, originalCountQueryResult: QueryResult): (QueryResult, Try[QueryResult]) = {
    originalCountQueryResult -> (for {
      countData <- getResultFromCrc(runQueryReq, originalCountQueryResult.resultId)
    } yield originalCountQueryResult.withSetSize(countData.metadata.setSize))
  }

  private[adapter] def attemptToRetrieveBreakdowns(runQueryReq: RunQueryRequest, breakdownResults: Seq[QueryResult]): Seq[(QueryResult, Try[QueryResult])] = {
    breakdownResults.map { origBreakdownResult =>
      origBreakdownResult -> (for {
        breakdownData <- getResultFromCrc(runQueryReq, origBreakdownResult.resultId).map(_.data)
      } yield origBreakdownResult.withBreakdown(breakdownData))
    }
  }

  private[adapter] def logBreakdownFailures(response: RawCrcRunQueryResponse,
    failures: Seq[(QueryResult, Try[QueryResult])]) {
    for {
      (origQueryResult, Failure(e)) <- failures
    } {
      error(s"Couldn't load breakdown for QueryResult with masterId: ${response.queryId}, instanceId: ${origQueryResult.instanceId}, resultId: ${origQueryResult.resultId}. Asked for result type: ${origQueryResult.resultType}", e)
    }
  }

  private def isLockedOut(authn: AuthenticationInfo): Boolean = {
    adapterLockoutAttemptsThreshold match {
      case 0 => false
      case _ => dao.isUserLockedOut(authn, adapterLockoutAttemptsThreshold)
    }
  }

  private def logStartup(): Unit = {
    val message = {
      if (runQueriesImmediately) { s"${getClass.getSimpleName} will run queries immediately" }
      else { s"${getClass.getSimpleName} will queue queries for later execution" }
    }

    info(message)
  }
}

object RunQueryAdapter {
  private[adapter] def obfuscateBreakdowns(breakdowns: Map[ResultOutputType, I2b2ResultEnvelope]): Map[ResultOutputType, I2b2ResultEnvelope] = {
    breakdowns.mapValues(_.mapValues(Obfuscator.obfuscate))
  }
}

case class ErrorFromCrcBreakdown(x:ErrorFromCrcException) extends AbstractProblem(ProblemSources.Adapter) {

  override lazy val throwable = Some(x)
  override lazy val summary: String = "The CRC reported an error."
  override lazy val description = "The CRC reported an internal error."
}

case class CannotInterpretCrcBreakdownXml(x:MissingCrCXmlResultException) extends AbstractProblem(ProblemSources.Adapter) {

  override lazy val throwable = Some(x)
  override lazy val summary: String = "SHRINE cannot interpret the CRC response."
  override lazy val description = "The CRC responded, but SHRINE could not interpret that response."
}