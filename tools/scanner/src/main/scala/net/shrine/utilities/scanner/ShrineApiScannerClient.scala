package net.shrine.utilities.scanner

import net.shrine.client.ShrineClient
import net.shrine.log.Loggable
import scala.concurrent.Future
import scala.concurrent.blocking
import scala.concurrent.ExecutionContext
import net.shrine.protocol.query.Term
import net.shrine.protocol.QueryResult
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.AggregatedRunQueryResponse
import java.util.concurrent.Executors
import ScannerClient._
import net.shrine.authentication.NotAuthenticatedException
import net.shrine.authentication.Authenticator
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.authentication.AuthenticationResult

/**
 * @author clint
 * @date Mar 12, 2013
 */
class ShrineApiScannerClient(val shrineClient: ShrineClient, override val authenticator: Authenticator, override val authn: AuthenticationInfo)(implicit executionContext: ExecutionContext) extends ScannerClient with Loggable {
  private val shouldBroadcast = false

  override def query(term: String): Future[TermResult] = afterAuthenticating { authResult =>
    Future {
      blocking {
        import Scanner.QueryDefaults._

        info(s"Querying for '$term'")

        val aggregatedResults: AggregatedRunQueryResponse = shrineClient.runQuery(topicId, outputTypes, toQueryDef(term), shouldBroadcast)

        aggregatedResults.results.headOption match {
          case None => errorTermResult(aggregatedResults.queryId, term)
          case Some(queryResult) => TermResult(aggregatedResults.queryId, queryResult.resultId, term, queryResult.statusType, queryResult.setSize)
        }
      }
    }
  }

  override def retrieveResults(termResult: TermResult): Future[TermResult] = afterAuthenticating { authResult =>
    Future {
      blocking {
        info(s"Retrieving results for previously-incomplete query for '${termResult.term}'")

        val aggregatedResults = shrineClient.readQueryResult(termResult.networkResultId, shouldBroadcast)

        aggregatedResults.results.headOption match {
          case None => errorTermResult(aggregatedResults.queryId, termResult.term)
          case Some(queryResult) => TermResult(aggregatedResults.queryId, queryResult.resultId, termResult.term, queryResult.statusType, queryResult.setSize)
        }
      }
    }
  }
}