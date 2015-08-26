package net.shrine.aggregation

import scala.concurrent.duration.DurationInt

import org.junit.Test
import net.shrine.util.ShouldMatchersForJUnit
import org.scalatest.mock.EasyMockSugar

import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.protocol.NodeId
import net.shrine.protocol.QueryMaster
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.protocol.Result
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlGcEnrichments

/**
 * @author Bill Simons
 * @date 6/8/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
final class ReadPreviousQueriesAggregatorTest extends ShouldMatchersForJUnit with EasyMockSugar {
  private def newQueryMasterType(groupId: String, userId: String, masterId: String, networkId: Long, name: String, createDate: XMLGregorianCalendar): QueryMaster = {
    QueryMaster(masterId, networkId, name, userId, groupId, createDate)
  }

  import scala.concurrent.duration._
  import XmlGcEnrichments._
  import org.easymock.EasyMock.{expect => invoke}
  
  @Test
  def testOldestToNewest {
    val groupId = "groupId"
    val userId = "userId"
    val aggregator = new ReadPreviousQueriesAggregator
    val date = XmlDateHelper.now
    
    val older = newQueryMasterType(groupId, userId, "1", 2L, "name1", date)
    val newer = newQueryMasterType(groupId, userId, "1", 2L, "name1", date + 1.day)

    aggregator.oldestToNewest(newer, older) should be(false)
    aggregator.oldestToNewest(newer, newer) should be(false)
    aggregator.oldestToNewest(older, newer) should be(true)
  }

  @Test
  def testNewestToOldest {
    val groupId = "groupId"
    val userId = "userId"
    val aggregator = new ReadPreviousQueriesAggregator
    val date = XmlDateHelper.now
    val older = newQueryMasterType(groupId, userId, "1", 2L, "name1", date)
    val newer = newQueryMasterType(groupId, userId, "1", 2L, "name1", date + 1.day)

    aggregator.newestToOldest(newer, older) should be(true)
    aggregator.newestToOldest(newer, newer) should be(false)
    aggregator.newestToOldest(older, newer) should be(false)
  }

  @Test
  def testAggregate {
    val userId = "userId"
    val groupId = "groupId"
    val firstDate = XmlDateHelper.now
    
    val firstQm = newQueryMasterType(groupId, userId, "1", 2L, "name1", firstDate)
    val lastQma = newQueryMasterType(groupId, userId, "2", 2L, "name2", firstDate + 2.days)
    val lastQmb = newQueryMasterType(groupId, userId, "2", 2L, "name2", firstDate + 1.day)
    val middleQm = newQueryMasterType(groupId, userId, "3", 2L, "name3", firstDate + 1.hour)
    
    val masters1 = Seq(firstQm, lastQma)
    val masters2 = Seq(lastQmb, middleQm)
    
    val response1 = ReadPreviousQueriesResponse(masters1)
    val response2 = ReadPreviousQueriesResponse(masters2)
    
    val result1 = Result(NodeId("A"), 1.second, response1)
    val result2 = Result(NodeId("B"), 2.seconds, response2)
    
    val aggregator = new ReadPreviousQueriesAggregator

    //TODO: test handling error responses
    val actual = aggregator.aggregate(Seq(result1, result2), Nil).asInstanceOf[ReadPreviousQueriesResponse]
    
    actual.isInstanceOf[ReadPreviousQueriesResponse] should be(true)

    actual.queryMasters.size should equal(3)
    actual.queryMasters(0) should equal(lastQmb)
    actual.queryMasters(0).createDate should equal(lastQmb.createDate)
    actual.queryMasters(1) should equal(middleQm)
    actual.queryMasters(2) should equal(firstQm)
  }
}