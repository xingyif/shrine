package net.shrine.aggregation

import net.shrine.aggregation.BasicAggregator.Valid
import net.shrine.log.Loggable
import net.shrine.protocol.{QueryMaster, ShrineResponse, ReadPreviousQueriesResponse}

/**
 * @author Bill Simons
 * @author Clint Gilbert
 * @date 6/8/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class ReadPreviousQueriesAggregator extends IgnoresErrorsAggregator[ReadPreviousQueriesResponse] with Loggable {

  private[aggregation] def newestToOldest(x: QueryMaster, y: QueryMaster) = x.createDate.compare(y.createDate) > 0
  
  private[aggregation] def oldestToNewest(x: QueryMaster, y: QueryMaster) = x.createDate.compare(y.createDate) < 0

  override def makeResponseFrom(responses: Iterable[Valid[ReadPreviousQueriesResponse]]): ShrineResponse = {
    //debug(s"Raw previous query responses: $responses")
    
    val mastersGroupedById = responses.flatMap(_.response.queryMasters).groupBy(_.queryMasterId)

    val sortedMastersById = mastersGroupedById.map { case (id, mastersWithThatId) => (id, mastersWithThatId.toSeq.sortWith(oldestToNewest)) }.toMap

    val mostRecentMastersForEachId = sortedMastersById.flatMap { case (id, mastersWithThatId) => mastersWithThatId.headOption }.toSeq

    val sortedMasters = mostRecentMastersForEachId.sortWith(newestToOldest)
    
    val result = ReadPreviousQueriesResponse(sortedMasters)
    
    debug("Previous queries: ")
    
    sortedMasters.foreach(debug(_))

    debug(s"Previous queries result: $result")

    result
  }
}