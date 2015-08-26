package net.shrine.adapter.components

import net.shrine.adapter.AdapterTestHelpers
import org.junit.Test
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import net.shrine.protocol.ReadPreviousQueriesResponse
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.adapter.service.CanLoadTestData
import net.shrine.adapter.HasI2b2AdminDao
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import javax.xml.datatype.XMLGregorianCalendar

/**
 * @author clint
 * @date Apr 23, 2013
 */
final class I2b2AdminPreviousQueriesTest extends AbstractSquerylAdapterTest with AdapterTestHelpers with ShouldMatchersForJUnit with CanLoadTestData with HasI2b2AdminDao {

  private val i2b2AdminPreviousQueriesSource = I2b2AdminPreviousQueries(i2b2AdminDao)

  private def get(req: ReadI2b2AdminPreviousQueriesRequest): ReadPreviousQueriesResponse = {
    i2b2AdminPreviousQueriesSource.get(req).asInstanceOf[ReadPreviousQueriesResponse]
  }

  import ReadI2b2AdminPreviousQueriesRequest.{ Username, Category }
  import Username._
  
  @Test
  def testReadPreviousQueriesExcludeUser: Unit = afterLoadingTestData {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Except(authn2.username), "", 10, None)

    val resp = get(request)

    resp.queryMasters.map(_.networkQueryId) should equal(Seq(networkQueryId2, networkQueryId1))
  }
  
  @Test
  def testReadPreviousQueriesOnlyFlagged: Unit = afterLoadingTestData {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, All, "", 10, None, categoryToSearchWithin = Category.Flagged)

    val resp = get(request)

    resp.queryMasters.map(_.networkQueryId) should equal(Seq(networkQueryId4, networkQueryId1))
  }
  
  @Test
  def testReadPreviousQueriesOnlyFlaggedExcludingUser: Unit = afterLoadingTestData {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Except(authn.username), "", 10, None, categoryToSearchWithin = Category.Flagged)

    val resp = get(request)

    resp.queryMasters.map(_.networkQueryId) should equal(Seq(networkQueryId4))
  }
  
  @Test
  def testReadPreviousQueriesExcludingUserWithSearchString: Unit = afterLoadingTestData {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, All, queryName1, 10, None, categoryToSearchWithin = Category.Flagged)

    val resp = get(request)

    resp.queryMasters.map(_.networkQueryId) should equal(Seq(networkQueryId1))
  }
  
  @Test
  def testReadPreviousQueriesNoMatch = afterCreatingTables {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "some-query-name-that-doesn't-exist", 10, None)

    val resp = get(request)

    resp.queryMasters should equal(Nil)
  }

  @Test
  def testReadPreviousQueriesAscending = doTestReadPreviousQueries(ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, Seq(masterId1, masterId2))

  @Test
  def testReadPreviousQueriesDescending = doTestReadPreviousQueries(ReadI2b2AdminPreviousQueriesRequest.SortOrder.Descending, Seq(masterId2, masterId1))

  private def doTestReadPreviousQueries(order: ReadI2b2AdminPreviousQueriesRequest.SortOrder, expectedIds: Seq[String], modify: ReadI2b2AdminPreviousQueriesRequest => ReadI2b2AdminPreviousQueriesRequest = r => r) = afterLoadingTestData {
    val baseRequest = modify(ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly(authn.username), "query-name", 10, None, order))

    {
      val req = baseRequest

      val resp = get(req)

      resp.queryMasters.map(_.queryMasterId) should equal(expectedIds)
    }

    {
      val req = baseRequest.copy(maxResults = 1)

      val resp = get(req)

      resp.queryMasters.map(_.queryMasterId) should equal(Seq(expectedIds.head))
    }
  }

  @Test
  def testReadPreviousQueriesStartsWith {
    doTestReadPreviousQueries(ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, Seq(masterId1, masterId2), r => r.copy(searchStrategy = ReadI2b2AdminPreviousQueriesRequest.Strategy.Left))

    afterLoadingTestData {
      val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "foo", 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, ReadI2b2AdminPreviousQueriesRequest.Strategy.Left)

      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters should equal(Nil)
    }
  }

  @Test
  def testReadPreviousQueriesEndsWith {
    afterLoadingTestData {
      val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly(authn.username), "1", 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, ReadI2b2AdminPreviousQueriesRequest.Strategy.Right)

      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId1))
    }

    afterLoadingTestData {
      val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "foo", 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, ReadI2b2AdminPreviousQueriesRequest.Strategy.Right)

      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters should equal(Nil)
    }
  }

  @Test
  def testReadPreviousQueriesExact = afterLoadingTestData {
    {
      val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), queryName1, 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, ReadI2b2AdminPreviousQueriesRequest.Strategy.Exact)

      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId1))
    }

    {
      val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "foo", 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, ReadI2b2AdminPreviousQueriesRequest.Strategy.Right)

      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters should equal(Nil)
    }

    //Test with specific usernames
    {
      val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly(authn2.username), queryName2, 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, ReadI2b2AdminPreviousQueriesRequest.Strategy.Exact)

      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId3, masterId4))
    }

    {
      val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly(authn2.username), queryName1, 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, ReadI2b2AdminPreviousQueriesRequest.Strategy.Right)

      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters should equal(Nil)
    }
  }

  @Test
  def testReadPreviousQueriesContains = afterLoadingTestData {
    {
      val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "-name", 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, ReadI2b2AdminPreviousQueriesRequest.Strategy.Contains)

      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId1, masterId2, masterId3, masterId4))
    }

    {
      val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "foo", 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending, ReadI2b2AdminPreviousQueriesRequest.Strategy.Contains)

      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters should equal(Nil)
    }
  }

  @Test
  def testReadPreviousQueriesStartDateIsRespected = afterLoadingTestData {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "query", 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending)

    //Should return everything
    {
      val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId1, masterId2, masterId3, masterId4))
    }

    val allStoredQueries = list(queryRows)

    def findStartDate(networkQueryId: Long): Option[XMLGregorianCalendar] = allStoredQueries.find(_.networkId == networkQueryId).map(_.dateCreated)

    val query1StartDate = findStartDate(networkQueryId1)
    val query2StartDate = findStartDate(networkQueryId2)
    val query3StartDate = findStartDate(networkQueryId3)
    val query4StartDate = findStartDate(networkQueryId4)

    {
      val shouldReturnLastOne = request.copy(startDate = query3StartDate)

      val resp = get(shouldReturnLastOne).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId4))
    }

    {
      val shouldReturnLastTwo = request.copy(startDate = query2StartDate)

      val resp = get(shouldReturnLastTwo).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId3, masterId4))
    }

    {
      val shouldReturnLastThree = request.copy(startDate = query1StartDate)

      val resp = get(shouldReturnLastThree).asInstanceOf[ReadPreviousQueriesResponse]

      resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId2, masterId3, masterId4))
    }
  }

  @Test
  def testWorkbenchPagingAscending = afterLoadingTestData {

    def firstCreateDate(resp: ReadPreviousQueriesResponse) = resp.queryMasters.headOption.map(_.createDate)
    
    import I2b2AdminPreviousQueriesTest.Implicits._
    
    //NB: Match query Nich was running that he said didn't work: username is "@", empty search string, matching by 'contains', 
    val req1 = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "", 1, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending)

    //Should return first query
    val resp1 = get(req1)

    resp1.shouldHaveOnly(masterId1)
    
    //Should return second query
    val req2 = req1.copy(startDate = firstCreateDate(resp1))

    val resp2 = get(req2)
    
    resp2.shouldHaveOnly(masterId2)
    
    //Should return third query
    val req3 = req2.copy(startDate = firstCreateDate(resp2))

    val resp3 = get(req3)
    
    resp3.shouldHaveOnly(masterId3)

    //Should return fourth query
    val req4 = req3.copy(startDate = firstCreateDate(resp3))

    val resp4 = get(req4)
    
    resp4.shouldHaveOnly(masterId4)
    
    //No more queries, should return nothing
    val req5 = req4.copy(startDate = firstCreateDate(resp4))

    val resp5 = get(req5)
    
    resp5.queryMasters should equal(Nil)
  }
  
  @Test
  def testWorkbenchPagingDescending = afterLoadingTestData {

    def firstCreateDate(resp: ReadPreviousQueriesResponse) = resp.queryMasters.headOption.map(_.createDate)
    
    import I2b2AdminPreviousQueriesTest.Implicits._
    
    //NB: Match query Nich was running that he said didn't work: username is "@", empty search string, matching by 'contains', 
    val req1 = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "", 1, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Descending)

    //Should return first query
    val resp1 = get(req1)

    resp1.shouldHaveOnly(masterId4)
    
    //Should return second query
    val req2 = req1.copy(startDate = firstCreateDate(resp1))

    val resp2 = get(req2)
    
    resp2.shouldHaveOnly(masterId3)
    
    //Should return third query
    val req3 = req2.copy(startDate = firstCreateDate(resp2))

    val resp3 = get(req3)
    
    resp3.shouldHaveOnly(masterId2)

    //Should return fourth query
    val req4 = req3.copy(startDate = firstCreateDate(resp3))

    val resp4 = get(req4)
    
    resp4.shouldHaveOnly(masterId1)
    
    //No more queries, should return nothing
    val req5 = req4.copy(startDate = firstCreateDate(resp4))

    val resp5 = get(req5)
    
    resp5.queryMasters should equal(Nil)
  }

  @Test
  def testCorrectUserAndGroupIdsAreReturnedForEachMaster {
    val request = ReadI2b2AdminPreviousQueriesRequest(projectId, waitTime, authn, Exactly("@"), "query", 10, None, ReadI2b2AdminPreviousQueriesRequest.SortOrder.Ascending)

    //Should return everything
    val resp = get(request).asInstanceOf[ReadPreviousQueriesResponse]

    resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId1, masterId2, masterId3, masterId4))

    resp.queryMasters.map(_.userId) should equal(Seq(authn.username, authn.username, authn2.username, authn2.username))
    resp.queryMasters.map(_.groupId) should equal(Seq(authn.domain, authn.domain, authn2.domain, "some-completely-different-domain"))
  }
}

object I2b2AdminPreviousQueriesTest extends ShouldMatchersForJUnit {
  object Implicits {
    implicit final class ResponseHelpers(val resp: ReadPreviousQueriesResponse) extends AnyVal {
      def shouldHaveOnly(masterId: String) {
        resp.queryMasters.map(_.queryMasterId) should equal(Seq(masterId))
      }
    }
  }
}
