package net.shrine.utilities.batchquerier

import net.shrine.protocol.QueryResult.StatusType.Error

import scala.concurrent.duration.Duration
import javax.xml.datatype.XMLGregorianCalendar

import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.version.v24.AggregatedRunQueryResponse

/**
 * @author clint
 * @date Sep 10, 2013
 */
object BatchQueryResults {
  def fromAggregatedRunQueryResponse(response: AggregatedRunQueryResponse): Iterable[BatchQueryResult] = {
    response.results.map { queryResult =>
      import scala.concurrent.duration._
      
      BatchQueryResult(
          queryResult.description.getOrElse("Unknown institution"), 
          response.requestXml,
          queryResult.statusType,
          queryResult.elapsed.map(_.milliseconds).getOrElse(Duration.Undefined), 
          queryResult.setSize)
    }
  }
  
  def forFailure(queryDef: QueryDefinition): BatchQueryResult = {
    import scala.concurrent.duration._
    
    BatchQueryResult("N/A", queryDef, Error, 0.milliseconds, -1)
  }
}