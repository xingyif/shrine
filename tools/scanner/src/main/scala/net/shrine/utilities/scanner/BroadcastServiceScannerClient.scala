package net.shrine.utilities.scanner

import net.shrine.log.Loggable

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import Scanner.QueryDefaults.outputTypes
import Scanner.QueryDefaults.topicId
import ScannerClient.errorTermResult
import ScannerClient.toQueryDef
import net.shrine.aggregation.Aggregators
import net.shrine.aggregation.ReadQueryResultAggregator
import net.shrine.broadcaster.BroadcastAndAggregationService
import net.shrine.protocol.AggregatedReadQueryResultResponse
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.ReadQueryResultRequest
import net.shrine.protocol.RunQueryRequest
import scala.concurrent.duration.Duration
import net.shrine.authentication.AuthenticationResult
import net.shrine.protocol.Credential
import net.shrine.authentication.Authenticator
import net.shrine.authentication.NotAuthenticatedException

/**
 * @author clint
 * @date Mar 14, 2013
 */
class BroadcastServiceScannerClient(
    val projectId: String, 
    override val authn: AuthenticationInfo, 
    val broadcastAndAggregationService: BroadcastAndAggregationService, 
    override val authenticator: Authenticator, 
    implicit val executionContext: ExecutionContext) extends ScannerClient with Loggable { 
  
  //TODO: Make this configurable
  private val waitTime: Duration = {
    import scala.concurrent.duration._
    
    10.seconds
  }
  
  //Don't ask for an aggregated (summed) result, since we'll get at most one result back in any case 
  private val runQueryAggregatorSource = Aggregators.forRunQueryRequest(false) _
  
  private def toAuthn(authResult: AuthenticationResult.Authenticated) = AuthenticationInfo(authResult.domain, authResult.username, Credential("", false)) 
  
  override def query(term: String): Future[TermResult] = afterAuthenticating { authResult =>
    import Scanner.QueryDefaults._

    info(s"Querying for '$term'")
    
    val request = RunQueryRequest(projectId, waitTime, authn, Option(topicId), Option(topicName), outputTypes, toQueryDef(term))
    
    val futureResponse = broadcastAndAggregationService.sendAndAggregate(toAuthn(authResult), request, runQueryAggregatorSource(request), false)
    
    def toTermResult(runQueryResponse: AggregatedRunQueryResponse): TermResult = {
      val termResultOption = for {
        shrineQueryResult <- runQueryResponse.results.headOption
      } yield TermResult(runQueryResponse.queryId, shrineQueryResult.resultId, term, shrineQueryResult.statusType, shrineQueryResult.setSize)
      
      //TODO: Is this the right query id to use here?
      termResultOption.getOrElse(errorTermResult(runQueryResponse.queryId, term))
    }
    
    futureResponse.collect { case resp: AggregatedRunQueryResponse => resp }.map(toTermResult)
  }
  
  override def retrieveResults(termResult: TermResult): Future[TermResult] = afterAuthenticating { authResult =>
    info(s"Retrieving results for previously-incomplete query for '${termResult.term}'")
    
    val request = ReadQueryResultRequest(projectId, waitTime, authn, termResult.networkQueryId)
    
    val futureResponse = broadcastAndAggregationService.sendAndAggregate(toAuthn(authResult), request, new ReadQueryResultAggregator(termResult.networkQueryId, false), false)
    
    def toTermResult(readQueryResultResponse: AggregatedReadQueryResultResponse): TermResult = {
      val termResultOption = for {
        shrineQueryResult <- readQueryResultResponse.results.headOption
      } yield {
        def elapsed = for {
          start <- shrineQueryResult.startDate.map(_.toGregorianCalendar.getTimeInMillis)
          end <- shrineQueryResult.endDate.map(_.toGregorianCalendar.getTimeInMillis)
        } yield end - start
        
        debug(s"CRC Query result: ${ shrineQueryResult.statusType }: (${ elapsed } ms) '${ shrineQueryResult.description.getOrElse("No Description") }', '${ shrineQueryResult.statusMessage.getOrElse("No status message") }'")
        
        termResult.copy(status = shrineQueryResult.statusType, count = shrineQueryResult.setSize)
      }
      
      //TODO: Is this the right query id to use here?
      termResultOption.getOrElse(errorTermResult(termResult.networkQueryId, termResult.term))
    }
    
    futureResponse.collect { case resp: AggregatedReadQueryResultResponse => resp }.map(toTermResult)
  }
}