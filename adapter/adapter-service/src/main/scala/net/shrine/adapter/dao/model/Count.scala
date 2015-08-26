package net.shrine.adapter.dao.model

import scala.concurrent.duration.DurationLong

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.QueryResult
import net.shrine.protocol.QueryResult.StatusType
import net.shrine.protocol.ResultOutputType
import net.shrine.util.XmlGcEnrichments

/**
 * @author clint
 * @date Oct 16, 2012
 */
final case class Count(
  id: Int,
  resultId: Int,
  localId: Long,
  statusType: StatusType,
  creationDate: XMLGregorianCalendar,
  data: Option[CountData]) extends HasResultId {

  import ResultOutputType._

  private val resultType = Some(PATIENT_COUNT_XML)

  def toQueryResult: Option[QueryResult] = {
    val startDateOption = data.map(_.startDate)
    val endDateOption = data.map(_.endDate)

    data.map { countData =>
      QueryResult(localId,
        resultId, //Is this ok?  This field is supposed to be an i2b2 instanceId, but we're passing in an id from the new Shrine adapter DB
        resultType,
        countData.obfuscatedValue,
        startDateOption,
        endDateOption,
        //no desc
        None,
        statusType,
        // no status message
        None)
    }
  }
}

object Count {
  //TODO: Should this take a QueryResultRow and an Option[CountRow]?
  def fromRows(resultRow: QueryResultRow, countRow: CountRow): Count = {
    import XmlGcEnrichments._
    import scala.concurrent.duration._

    val elapsed = resultRow.elapsed.getOrElse(0L)

    Count(
      countRow.id,
      countRow.resultId,
      resultRow.localId,
      resultRow.status,
      countRow.creationDate,
      //NB: No CountData if the query is still in progress (PROCESSING, QUEUED)
      if (!resultRow.status.isDone) { None }
      else {
        Some(
          CountData(
            countRow.originalValue,
            countRow.obfuscatedValue,
            //NB: This loses the original starttime from the CRC, but preserves the ability to compute elapsed
            //times, which is all anyone cares about.  We need to be able to turn this into a QueryResult (with
            //start and end times) so we can't just include the elapsed time
            countRow.creationDate,
            countRow.creationDate + elapsed.milliseconds))
      })
  }
}

