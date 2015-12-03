package net.shrine.utilities.batchquerier.commands

import scala.concurrent.duration.Duration
import net.shrine.utilities.batchquerier.csv.CsvRow
import net.shrine.utilities.commands.>>>
import net.shrine.utilities.batchquerier.RepeatedBatchQueryResult

/**
 * @author clint
 * @date Oct 8, 2013
 */
object FormatForOutput extends (Iterable[RepeatedBatchQueryResult] >>> Iterable[CsvRow]) {
  def toTitleCase(s: String): String = s match {
    case "" => ""
    case _ => s.head.toUpper +: s.tail.toLowerCase
  }
  
  //TODO: TEST
  def toFormattedSeconds(duration: Duration): String = {
    if(duration.isFinite) {
      (duration.toMillis / 1000.0).formatted("%.2f") //%.2f means 2 decimal places
    } else { "NaN" }
  }
  
  override def apply(results: Iterable[RepeatedBatchQueryResult]): Iterable[CsvRow] = {
    results.map { result =>
      CsvRow(result.query.name, result.institution, toTitleCase(result.disposition.name), result.count, toFormattedSeconds(result.elapsed), toFormattedSeconds(result.meanDuration), result.numQueriesPerformed, result.query.expr.map(_.toXmlString).getOrElse("<none>"))
    }
  }
}