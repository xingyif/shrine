package net.shrine.utilities.batchquerier

import net.shrine.log.Loggable
import net.shrine.protocol.{AggregatedRunQueryResponse, ResultOutputType, RunQueryResponse}
import net.shrine.protocol.query.QueryDefinition

import scala.util.Try
import scala.util.Failure

/**
 * @author clint
 * @date Sep 6, 2013
 */
trait ShrineBatchQuerier extends BatchQuerier with Loggable { self: HasShrineClient with HasBatchQuerierConfig =>

  override def query(queryDefs: Iterable[QueryDefinition], runsPerQueryDef: Int): Iterable[QueryAttempt] = {
    val shouldBroadcast = true

    import ShrineBatchQuerier.Defaults._
    
    def queryFor(i: Int, queryDef: QueryDefinition): QueryAttempt = {
      val result = Try {
        info(s"($i/$runsPerQueryDef) Running query '${queryDef.name}': ${queryDef.expr}")
        
        client.runQuery(config.topicId, outputTypes, queryDef, shouldBroadcast)
      }
      
      result match {
        case Failure(e) => warn(s"Error running query $queryDef:", e)
        case _ => ()
      }
      
      QueryAttempt(queryDef, result)
    }
    
    for {
      queryDef <- queryDefs
      i <- 1 to runsPerQueryDef
    } yield queryFor(i, queryDef)
  }
}

object ShrineBatchQuerier {
  object Defaults {
    //TODO: Should this be configurable?
    val outputTypes = Set(ResultOutputType.PATIENT_COUNT_XML)
  }
}