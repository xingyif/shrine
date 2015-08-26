package net.shrine.utilities.batchquerier.commands

import org.junit.Test
import net.shrine.utilities.batchquerier.BatchQuerier
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlGcEnrichments
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.QueryResult
import net.shrine.protocol.query.Term
import net.shrine.utilities.batchquerier.BatchQueryResult
import net.shrine.utilities.batchquerier.BatchQuerierConfig
import scala.util.Try
import scala.util.Failure
import net.shrine.utilities.batchquerier.QueryAttempt
import net.shrine.util.ShouldMatchersForJUnit

/**
 * @author clint
 * @date Oct 9, 2013
 */
final class QueryWithTest extends ShouldMatchersForJUnit {
  @Test
  def testApply {
    import ResultOutputType.PATIENT_COUNT_XML
    
    def failureResult(queryDef: QueryDefinition) = {
      import scala.concurrent.duration._
      
      BatchQueryResult("N/A", queryDef, QueryResult.StatusType.Error, 0.milliseconds, -1)
    }
    
    object MockBatchQuerier extends BatchQuerier {
      
      var passedInQueryDefs: Iterable[QueryDefinition] = _
      
      override def query(queryDefs: Iterable[QueryDefinition], queriesPerTerm: Int): Iterable[QueryAttempt] = {
        
        passedInQueryDefs = queryDefs
        
        val masterId = 123L
          val instanceId = 456L
          val resultId = 789L
          val setSize = 99L
          val elapsedMillis = 50
          val start = XmlDateHelper.now
          val end = {
            import XmlGcEnrichments._
            import scala.concurrent.duration._  
            
            start + elapsedMillis.milliseconds
          }
        
        val responseAttempts = for {
          queryDef <- queryDefs
          _ <- 1 to queriesPerTerm
          queryResults = (1 to 3).map(i => QueryResult(resultId, instanceId, Option(PATIENT_COUNT_XML), setSize, Option(start), Option(end), Some(s"Institution $i"), QueryResult.StatusType.Finished, None))
        } yield {
          QueryAttempt(queryDef, Try(AggregatedRunQueryResponse(masterId, XmlDateHelper.now, "some-userId", "some-groupId", queryDef, instanceId, queryResults)))
        }
        
        val someFailures = queryDefs.map(queryDef => QueryAttempt(queryDef, Failure(new Exception with scala.util.control.NoStackTrace)))
        
        //NB: Add some failures to see they're properly handled
        someFailures ++ responseAttempts ++ someFailures
      }
    }
    
    val queryWith = QueryWith(MockBatchQuerier, 3)
    
    queryWith(Nil) should be(Nil)
    
    MockBatchQuerier.passedInQueryDefs should be(Nil)
    
    val queryDefs = (1 to 3).map(i => QueryDefinition(i.toString, Term(i.toString)))
    
    val responses = queryWith(queryDefs)
    
    MockBatchQuerier.passedInQueryDefs should be(queryDefs)
    
    val masterId = 123L
    val instanceId = 456L
    val resultId = 789L
    val setSize = 99L
    
    val Seq(queryDef1, queryDef2, queryDef3) = queryDefs
    
    import scala.concurrent.duration._  
    
    val expectedElapsed = 50.milliseconds
    
    import QueryResult.StatusType.Finished
    
    //NB: 3 institutions in the "network", 3 runs per querydef
    val expectedResponses = Seq(
        BatchQueryResult("Institution 1", queryDef1, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 2", queryDef1, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 3", queryDef1, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 1", queryDef1, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 2", queryDef1, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 3", queryDef1, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 1", queryDef1, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 2", queryDef1, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 3", queryDef1, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 1", queryDef2, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 2", queryDef2, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 3", queryDef2, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 1", queryDef2, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 2", queryDef2, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 3", queryDef2, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 1", queryDef2, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 2", queryDef2, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 3", queryDef2, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 1", queryDef3, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 2", queryDef3, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 3", queryDef3, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 1", queryDef3, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 2", queryDef3, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 3", queryDef3, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 1", queryDef3, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 2", queryDef3, Finished, expectedElapsed, setSize),
        BatchQueryResult("Institution 3", queryDef3, Finished, expectedElapsed, setSize),

        failureResult(queryDef1),
        failureResult(queryDef2),
        failureResult(queryDef3),
        failureResult(queryDef1),
        failureResult(queryDef2),
        failureResult(queryDef3))
        
    responses should equal(expectedResponses)
  }
}