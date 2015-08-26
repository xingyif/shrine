package net.shrine.adapter.dao.model

import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.QueryResult
import net.shrine.protocol.I2b2ResultEnvelope


/**
 * @author clint
 * @date Oct 16, 2012
 *
 * NB: Named ShrineQueryResult to avoid clashes with net.shrine.protocol.QueryResult
 * NB: Can't be final, since Squeryl runs this class through cglib to make a synthetic subclass :(
 */
final case class ShrineQueryResult(
  networkQueryId: Long,
  localId: String,
  wasRun: Boolean,
  isFlagged: Boolean,
  flagMessage: Option[String],
  count: Count,
  breakdowns: Seq[Breakdown],
  errors: Seq[ShrineError]) {
  
  //TODO: include breakdowns as well?  What if they're PROCESSING while the count is FINISHED?  Can this even happen?
  val isDone = count.statusType.isDone
  
  def wasNotRun: Boolean = !wasRun
  
  def toQueryResults(doObfuscation: Boolean): Option[QueryResult] = {
    val countResult = count.toQueryResult.map { countQueryResult =>
      //add breakdowns
      
      val byType = Map.empty ++ breakdowns.map(b => (b.resultType, b.data))
      
      val getRealOrObfuscated: ObfuscatedPair => Long = { 
        if(doObfuscation) { _.obfuscated }
        else { _.original }
      }
      
      val typesToData = byType.mapValues(_.mapValues(getRealOrObfuscated))
      
      countQueryResult.withBreakdowns(typesToData.map { 
        case (resultType, data) => 
          (resultType, I2b2ResultEnvelope(resultType, data)) 
      })
    }
    
    def firstError = errors.headOption.map(_.toQueryResult)
    
    countResult orElse firstError
  }
}

object ShrineQueryResult {
  def fromRows(queryRow: ShrineQuery, resultRows: Seq[QueryResultRow], countRow: CountRow, breakdownRows: Map[ResultOutputType, Seq[BreakdownResultRow]], errorRows: Seq[ShrineError]): Option[ShrineQueryResult] = {
    if(resultRows.isEmpty) {
      None
    } else {
      val resultRowsByType = resultRows.map(r => r.resultType -> r).toMap
      
      val breakdowns = (for {
        (resultType, resultRow) <- resultRowsByType
        rows <- breakdownRows.get(resultType)
        breakdown <- Breakdown.fromRows(resultType, resultRow.localId, rows)
      } yield breakdown).toSeq

      import ResultOutputType.PATIENT_COUNT_XML
      
      for {
        resultRow <- resultRowsByType.get(PATIENT_COUNT_XML)
        count = Count.fromRows(resultRow, countRow) 
      } yield ShrineQueryResult(queryRow.networkId, queryRow.localId, queryRow.hasBeenRun, queryRow.isFlagged, queryRow.flagMessage, count, breakdowns, errorRows)
    }
  }
}