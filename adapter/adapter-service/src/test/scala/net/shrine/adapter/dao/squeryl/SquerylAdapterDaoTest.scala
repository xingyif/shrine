package net.shrine.adapter.dao.squeryl

import net.shrine.problem.TestProblem
import net.shrine.util.ShouldMatchersForJUnit
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.Credential
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.query.Term
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.I2b2ResultEnvelope
import net.shrine.protocol.QueryResult
import net.shrine.protocol.RawCrcRunQueryResponse
import org.junit.Test
import net.shrine.adapter.dao.model.ShrineQuery
import net.shrine.adapter.dao.model.ShrineQueryResult
import net.shrine.adapter.dao.model.ObfuscatedPair
import net.shrine.adapter.dao.model.QueryResultRow
import net.shrine.protocol.DefaultBreakdownResultOutputTypes
import net.shrine.adapter.dao.model.squeryl.SquerylShrineQuery
import net.shrine.dao.DateHelpers

/**
 * @author clint
 * @since May 24, 2013
 */
final class SquerylAdapterDaoTest extends AbstractSquerylAdapterTest with ShouldMatchersForJUnit {
  private val authn = AuthenticationInfo("some-domain", "some-user", Credential("laskhdakslhd", false))

  private val queryDef1 = QueryDefinition("foo", Term("blarg"))

  private def now = Option(XmlDateHelper.now)

  import ResultOutputType._
  import DefaultBreakdownResultOutputTypes._

  private val topicId = "some-topic-id"
  private val count = 999L
  private val resultId = 1L
  private val instanceId = 2L
  private val desc = Some("xyz")
  private val message1 = "something bad happened"
  private val message2 = "blah blah blah"

  private val onlyAgeBreakdown = Map(PATIENT_AGE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_AGE_COUNT_XML, Map("x" -> 1, "y" -> 2)))
  private val onlyGenderBreakdown = Map(PATIENT_GENDER_COUNT_XML -> I2b2ResultEnvelope(PATIENT_GENDER_COUNT_XML, Map("a" -> 123, "b" -> 456)))

  private val breakdownsByType = onlyAgeBreakdown ++ onlyGenderBreakdown

  private val networkQueryId1 = 123L
  private val networkQueryId2 = 456L
  private val networkQueryId3 = 999L

  private val masterId1 = "abc"
  private val masterId2 = "def"
  private val masterId3 = "ghi"

  private val countQueryResult = QueryResult(resultId, instanceId, Some(PATIENT_COUNT_XML), count, now, now, desc, QueryResult.StatusType.Finished, None)

  private val errorQueryResult1 = QueryResult.errorResult(desc, message1,TestProblem)
  private val errorQueryResult2 = QueryResult.errorResult(desc, message2,TestProblem)

  private val breakdownQueryResult1 = QueryResult(resultId, instanceId, Some(PATIENT_AGE_COUNT_XML), countQueryResult.setSize, now, now, desc, QueryResult.StatusType.Finished, None, breakdowns = onlyAgeBreakdown)
  private val breakdownQueryResult2 = QueryResult(resultId, instanceId, Some(PATIENT_GENDER_COUNT_XML), countQueryResult.setSize, now, now, desc, QueryResult.StatusType.Finished, None, breakdowns = onlyGenderBreakdown)

  import RawCrcRunQueryResponse.toQueryResultMap

  private val countRunQueryResponse = RawCrcRunQueryResponse(networkQueryId1, now.get, authn.username, authn.domain, queryDef1, instanceId, toQueryResultMap(Seq(countQueryResult)))

  private val onlyErrorsRunQueryResponse = countRunQueryResponse.withResults(Seq(errorQueryResult1, errorQueryResult2))

  private val countAndBreakdownsRunQueryResponse = countRunQueryResponse.withResults(Seq(countQueryResult, breakdownQueryResult1, breakdownQueryResult2))

  private val onlyBreakdownsRunQueryResponse = countRunQueryResponse.withResults(Seq(breakdownQueryResult1, breakdownQueryResult2))

  @Test
  def testFindQueryByNetworkIdNullQueryDefNonNullQueryExpr: Unit = afterCreatingTables {
    
    val id = 123
    val localId = "localId"
    val networkQueryId = 42L
    val queryName = "some-query"
    val user = "u"
    val domain = "d"
    val expr = Term("""\\FOO\BAR\BAZ""")
    val dateCreated = XmlDateHelper.now
    val isFlagged = true
    val flagMessage = Some("fm")
    val hasBeenRun = true
    
    val squerylShrineQuery = SquerylShrineQuery(
      id,
      localId,
      networkQueryId,
      queryName,
      user,
      domain,
      Some(expr.toXmlString),
      DateHelpers.toTimestamp(dateCreated),
      isFlagged,
      flagMessage,
      hasBeenRun,
      queryXml = None)

    tables.shrineQueries.insert(squerylShrineQuery)
      
    val shrineQuery = squerylShrineQuery.toShrineQuery

    shrineQuery.dateCreated should equal(dateCreated)
    shrineQuery.domain should equal(domain)
    shrineQuery.username should equal(user)
    shrineQuery.flagMessage should equal(flagMessage)
    shrineQuery.id should equal(1)
    shrineQuery.isFlagged should equal(isFlagged)
    shrineQuery.localId should equal(localId)
    shrineQuery.name should equal(queryName)
    shrineQuery.networkId should equal(networkQueryId)
    shrineQuery.queryDefinition should equal(QueryDefinition(queryName, expr))
    
    val fromDb = dao.findQueryByNetworkId(networkQueryId).get
    
    fromDb should equal(shrineQuery)
  }
  
  @Test
  def testFindPreviousQueries = afterCreatingTables {
    dao.findRecentQueries(0) should equal(Nil)
    dao.findRecentQueries(1) should equal(Nil)

    dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    //Make next query happen 10 milliseconds later, so we can distinguish it from the one we just inserted 
    //(java.sql.Timestamps have 1ms resolution, it appears?)
    Thread.sleep(10)

    dao.findRecentQueries(0) should equal(Nil)

    {
      val Seq(query) = dao.findRecentQueries(1)

      query.networkId should equal(networkQueryId1)
    }

    dao.insertQuery(masterId2, networkQueryId2, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    //Make next query happen 10 milliseconds later, so we can distinguish it from the one we just inserted 
    //(java.sql.Timestamps, or perhaps XmlGregorianCalendars, have 1ms resolution?)
    Thread.sleep(10)

    dao.insertQuery(masterId3, networkQueryId3, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    //Should come back newest-queries-first
    dao.findRecentQueries(2).map(_.networkId) should equal(Seq(networkQueryId3, networkQueryId2))
  }

  @Test
  def testDeleteQuery = afterCreatingTables {
    //A RunQueryResponse with all types of QueryResults: count, breakdown, and error
    val response = countRunQueryResponse.withResults(Seq(countQueryResult, breakdownQueryResult1, breakdownQueryResult2, errorQueryResult1, errorQueryResult2))

    doTestFindResultsFor(response) { resultIdsByType =>
      insertCount(resultIdsByType)

      insertErrors(resultIdsByType)

      insertBreakdowns(resultIdsByType, breakdownsByType)
    } {
      (resultIdsByType, resultOption) =>
        val Some(result) = resultOption

        validateCount(resultIdsByType, result)

        validateBreakdowns(result)

        validateErrors(result)
    }

    dao.deleteQuery(networkQueryId1)

    dao.findQueryByNetworkId(networkQueryId1) should be(None)
  }

  @Test
  def testRenameQuery = afterCreatingTables {
    dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    {
      val Some(query) = dao.findQueryByNetworkId(networkQueryId1)

      query.name should equal(queryDef1.name)
    }

    val newName = "zuh"

    dao.renameQuery(networkQueryId1, newName)

    val Some(renamedQuery) = dao.findQueryByNetworkId(networkQueryId1)

    renamedQuery.name should equal(newName)
    renamedQuery.id should be(1)
    renamedQuery.networkId should equal(networkQueryId1)
    renamedQuery.username should equal(authn.username)
    renamedQuery.domain should equal(authn.domain)
    renamedQuery.dateCreated should not be (null)
    renamedQuery.queryDefinition.name should not equal(queryDef1.name)
    renamedQuery.queryDefinition.expr should equal(queryDef1.expr)
    renamedQuery.queryDefinition.timing should equal(queryDef1.timing)
    renamedQuery.queryDefinition.id should equal(queryDef1.id)
    renamedQuery.queryDefinition.queryType should equal(queryDef1.queryType)
    renamedQuery.queryDefinition.constraints should equal(queryDef1.constraints)
    renamedQuery.queryDefinition.subQueries should equal(queryDef1.subQueries)
    renamedQuery.isFlagged should be(false)
    renamedQuery.queryDefinition should not equal(queryDef1)
  }

  @Test
  def testFlagQuery = afterCreatingTables {
    dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    {
      val Some(query) = dao.findQueryByNetworkId(networkQueryId1)

      query.isFlagged should be(false)
    }

    dao.flagQuery(networkQueryId1, None)

    {
      val Some(flaggedQuery) = dao.findQueryByNetworkId(networkQueryId1)

      flaggedQuery.isFlagged should be(true)
      flaggedQuery.flagMessage should be(None)
    }

    val message = "askldjjkalshd"

    dao.flagQuery(networkQueryId1, Some(message))

    {
      val Some(flaggedQuery) = dao.findQueryByNetworkId(networkQueryId1)

      flaggedQuery.isFlagged should be(true)
      flaggedQuery.flagMessage should be(Some(message))
    }
  }

  @Test
  def testUnFlagQuery = afterCreatingTables {
    dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    {
      val Some(query) = dao.findQueryByNetworkId(networkQueryId1)

      query.isFlagged should be(false)
    }

    val message = "askdljasd"
    
    dao.flagQuery(networkQueryId1, Some(message))

    val Some(flaggedQuery) = dao.findQueryByNetworkId(networkQueryId1)

    flaggedQuery.isFlagged should be(true)
    flaggedQuery.flagMessage should be(Some(message))

    dao.unFlagQuery(networkQueryId1)

    val Some(unFlaggedQuery) = dao.findQueryByNetworkId(networkQueryId1)

    unFlaggedQuery.isFlagged should be(false)
    unFlaggedQuery.flagMessage should be(None)
  }

  @Test
  def testInsertQueryAndFindQueryByNetworkId = afterCreatingTables {
    dao.findQueryByNetworkId(networkQueryId1) should be(None)

    dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    {
      val Some(ShrineQuery(id, localMasterId, actualNetworkId, name, username, domain, dateCreated, isFlagged, flagMessage,queryDef)) = dao.findQueryByNetworkId(networkQueryId1)

      id should be(1)
      localMasterId should equal(masterId1)
      actualNetworkId should equal(networkQueryId1)
      name should equal(queryDef.name)
      username should equal(authn.username)
      domain should equal(authn.domain)
      dateCreated should not be (null) //NB: Don't compare, to avoid off-by-a-few errors :/
      isFlagged should be(false)
      queryDef should be(queryDef1)
    }

    //Inserting a query with the same values should be allowed, it should just create a new row with a new id
    dao.insertQuery(masterId1, networkQueryId1,  authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    {
      //However, the first shrine_query row with the passed networkQueryId should be returned, not the one we just inserted 
      val Some(ShrineQuery(id, localMasterId, actualNetworkId, name, username, domain, dateCreated, isFlagged, flagMessage,queryDef)) = dao.findQueryByNetworkId(networkQueryId1)

      id should be(1)
      localMasterId should equal(masterId1)
      actualNetworkId should equal(networkQueryId1)
      username should equal(authn.username)
      domain should equal(authn.domain)
      dateCreated should not be (null) //NB: Don't compare, to avoid off-by-a-few errors :/
      queryDef should equal(queryDef1)
      isFlagged should be(false)
    }
  }

  @Test
  def testFindQueriesByUserAndDomain: Unit = afterCreatingTables {
    dao.findQueriesByUserAndDomain("", "", 50) should equal(Nil)

    dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    //Make next query happen 10 milliseconds later, so we can distinguish it from the one we just inserted 
    //(java.sql.Timestamps have 1ms resolution, it appears?)
    Thread.sleep(10)

    dao.insertQuery(masterId2, networkQueryId2, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    dao.findQueriesByUserAndDomain("", "", 50) should equal(Nil)

    val Seq(foundQuery1, foundQuery2) = dao.findQueriesByUserAndDomain(authn.domain, authn.username, 50)

    foundQuery1.domain should equal(authn.domain)
    foundQuery1.username should equal(authn.username)
    foundQuery1.localId should equal(masterId2)
    foundQuery1.networkId should equal(networkQueryId2)
    foundQuery1.queryDefinition should equal(queryDef1)

    foundQuery2.domain should equal(authn.domain)
    foundQuery2.username should equal(authn.username)
    foundQuery2.localId should equal(masterId1)
    foundQuery2.networkId should equal(networkQueryId1)
    foundQuery2.queryDefinition should equal(queryDef1)

    val Seq(foundQuery) = dao.findQueriesByUserAndDomain(authn.domain, authn.username, 1)

    foundQuery should equal(foundQuery1)
  }

  @Test
  def testInsertQuery: Unit = {

    def doTestInsertQuery(flagged: Boolean, run: Boolean): Unit = afterCreatingTables {
      val ids = for (i <- 1 to 2) yield dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = flagged, hasBeenRun = run, flagMessage = None)

      ids should equal(Seq(1, 2))

      def testRow(r: ShrineQuery) {
        r.localId should equal(masterId1)
        r.networkId should equal(networkQueryId1)
        r.dateCreated should not be (null) // :/
        r.domain should equal(authn.domain)
        r.username should equal(authn.username)
        r.queryDefinition should equal(queryDef1)
        r.isFlagged should equal(flagged)
      }

      val rows = list(queryRows)

      val Seq(row1, row2) = rows

      row1.id should be(1)
      row2.id should be(2)

      rows.foreach(testRow)
    }

    doTestInsertQuery(true, true)
    doTestInsertQuery(true, false)
    doTestInsertQuery(false, true)
    doTestInsertQuery(false, false)
  }

  @Test
  def testInsertQueryResultsOnlyCount = afterCreatingTables {
    intercept[Exception] {
      //Should fail due to foreign key constraint
      dao.insertQueryResults(-1, countRunQueryResponse.results)
    }

    val (resultIdsByType, resultRows) = doInsertQueryResultsTest(countRunQueryResponse)

    val Seq(resultRow) = resultRows

    resultRow.resultType should equal(PATIENT_COUNT_XML)
    resultRow.status should equal(QueryResult.StatusType.Finished)

    resultIdsByType should equal(Map(PATIENT_COUNT_XML -> Seq(resultRow.id)))
  }

  @Test
  def testInsertQueryResultsOnlyErrors = afterCreatingTables {
    val (resultIdsByType, resultRows) = doInsertQueryResultsTest(onlyErrorsRunQueryResponse)

    resultRows.foreach { resultRow =>
      resultRow.elapsed should be(None)
      resultRow.resultType should equal(ERROR)
      resultRow.status should equal(QueryResult.StatusType.Error)
    }
  }

  @Test
  def testInsertQueryResultsNoErrorsOneCountSomeBreakdowns = afterCreatingTables {
    val (resultIdsByType, resultRows) = doInsertQueryResultsTest(countAndBreakdownsRunQueryResponse)

    resultIdsByType.keySet should equal(Set(PATIENT_COUNT_XML, PATIENT_AGE_COUNT_XML, PATIENT_GENDER_COUNT_XML))

    resultIdsByType.values.forall(_.size == 1) should be(true)

    val countResultRow = resultRows.filter(_.resultType == PATIENT_COUNT_XML).head

    countResultRow.resultType should equal(PATIENT_COUNT_XML)

    resultRows.filter(row => Set(PATIENT_AGE_COUNT_XML, PATIENT_GENDER_COUNT_XML).contains(row.resultType)).size should be(2)

    resultRows.foreach { resultRow =>
      resultRow.elapsed should not be (None)
      resultRow.status should equal(QueryResult.StatusType.Finished)
    }
  }

  private def doInsertQueryResultsTest(response: RawCrcRunQueryResponse): (Map[ResultOutputType, Seq[Int]], Seq[QueryResultRow]) = {
    list(queryResultRows).isEmpty should be(true)

    val queryId = dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    val resultIdsByType = dao.insertQueryResults(queryId, response.results)

    val resultRows = list(queryResultRows)

    resultRows.foreach { resultRow =>
      resultRow.elapsed should not be (null)
      resultRow.lastUpdated should not be (null)
      resultRow.queryId should equal(queryId)
    }

    list(errorResultRows).isEmpty should be(true)

    list(countResultRows).isEmpty should be(true)

    list(breakdownResultRows).isEmpty should be(true)

    (resultIdsByType, resultRows)
  }

  @Test
  def testInsertCountResult = afterCreatingTables {
    val queryId = dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    val response = RawCrcRunQueryResponse(networkQueryId1, now.get, authn.username, authn.domain, queryDef1, instanceId, toQueryResultMap(Seq(countQueryResult)))

    val resultIdsByType = dao.insertQueryResults(queryId, response.results)

    val Seq(resultRow) = list(queryResultRows)

    list(countResultRows).isEmpty should be(true)

    dao.insertCountResult(resultRow.id, countQueryResult.setSize, countQueryResult.setSize + 2)

    val Seq(countResultRow) = list(countResultRows)

    countResultRow.creationDate should not be (null)
    countResultRow.obfuscatedValue should equal(countQueryResult.setSize + 2)
    countResultRow.originalValue should equal(countQueryResult.setSize)
    countResultRow.resultId should equal(resultRow.id)

    intercept[Exception] {
      //Should fail due to foreign key constraint
      dao.insertCountResult(-12345, countQueryResult.setSize, countQueryResult.setSize + 2)
    }
  }

  @Test
  def testInsertBreakdownResults = afterCreatingTables {
    val (resultIdsByType, resultRows) = doInsertQueryResultsTest(onlyBreakdownsRunQueryResponse)

    resultIdsByType.keySet should equal(Set(PATIENT_AGE_COUNT_XML, PATIENT_GENDER_COUNT_XML))

    resultIdsByType.values.forall(_.size == 1) should be(true)

    val Seq(breakdownResultRow1, breakdownResultRow2) = resultRows

    dao.insertBreakdownResults(resultIdsByType, breakdownsByType, breakdownsByType.mapValues(_.mapValues(_ + 1)))

    val ageBreakdownRows = list(breakdownResultRows).filter(_.resultId == resultIdsByType(PATIENT_AGE_COUNT_XML).head)

    val genderBreakdownRows = list(breakdownResultRows).filter(_.resultId == resultIdsByType(PATIENT_GENDER_COUNT_XML).head)

    {
      val ageRowsByName = ageBreakdownRows.map(row => (row.dataKey, row)).toMap

      ageRowsByName("x").originalValue should equal(1)
      ageRowsByName("y").originalValue should equal(2)

      ageRowsByName("x").obfuscatedValue should equal(2)
      ageRowsByName("y").obfuscatedValue should equal(3)
    }

    {
      val genderRowsByName = genderBreakdownRows.map(row => (row.dataKey, row)).toMap

      genderRowsByName("a").originalValue should equal(123)
      genderRowsByName("b").originalValue should equal(456)

      genderRowsByName("a").obfuscatedValue should equal(124)
      genderRowsByName("b").obfuscatedValue should equal(457)
    }

    intercept[Exception] {
      //Should fail due to foreign key constraint
      dao.insertBreakdownResults(Map(PATIENT_AGE_COUNT_XML -> Seq(-1000)), breakdownsByType, breakdownsByType)
    }
  }

  @Test
  def testInsertErrorResult = afterCreatingTables {
    val queryId = dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    val response = countRunQueryResponse.withResults(Seq(errorQueryResult1))

    val resultIdsByType = dao.insertQueryResults(queryId, response.results)

    val Seq(resultRow) = list(queryResultRows)

    list(errorResultRows).isEmpty should be(true)

    val pd = TestProblem.toDigest

    dao.insertErrorResult(resultRow.id, message1,pd.codec,pd.stampText,pd.summary,pd.description,pd.detailsXml)

    val Seq(errorResultRow) = list(errorResultRows)

    errorResultRow.message should equal(message1)
    errorResultRow.resultId should equal(resultRow.id)

    intercept[Exception] {
      //Should fail due to foreign key constraint
      dao.insertErrorResult(-12345, "",pd.codec,pd.stampText,pd.summary,pd.description,pd.detailsXml)
    }
  }

  @Test
  def testFindResultsFor = afterCreatingTables {
    //A RunQueryResponse with all types of QueryResults: count, breakdown, and error
    val response = countRunQueryResponse.withResults(Seq(countQueryResult, breakdownQueryResult1, breakdownQueryResult2, errorQueryResult1, errorQueryResult2))

    doTestFindResultsFor(response) { resultIdsByType =>
      insertCount(resultIdsByType)

      insertErrors(resultIdsByType)

      insertBreakdowns(resultIdsByType, breakdownsByType)
    } {
      case (resultIdsByType, resultOption) =>
        val Some(result) = resultOption

        validateCount(resultIdsByType, result)

        validateBreakdowns(result)

        validateErrors(result)
    }
  }

  @Test
  def testFindResultsForOnlyCount = afterCreatingTables {
    //A RunQueryResponse with only a count QueryResult
    val response = countRunQueryResponse

    doTestFindResultsFor(response) { resultIdsByType =>
      insertCount(resultIdsByType)
    } {
      (resultIdsByType, resultOption) =>

        val Some(result) = resultOption

        validateCount(resultIdsByType, result)

        result.breakdowns.isEmpty should be(true)

        result.errors.isEmpty should be(true)
    }
  }

  @Test
  def testFindResultsForOnlyErrors = afterCreatingTables {
    //A RunQueryResponse with only errors
    val response = onlyErrorsRunQueryResponse

    doTestFindResultsFor(response) { resultIdsByType =>
      insertErrors(resultIdsByType)
    } {
      (resultIdsByType, resultOption) => resultOption should be(None)
    }
  }

  @Test
  def testFindResultsForOnlyBreakdowns = afterCreatingTables {
    //A RunQueryResponse with only breakdowns
    val response = countRunQueryResponse.withResults(Seq(countQueryResult, breakdownQueryResult1, breakdownQueryResult2, errorQueryResult1, errorQueryResult2))

    doTestFindResultsFor(response) { resultIdsByType =>
      insertBreakdowns(resultIdsByType, breakdownsByType)
    } {
      (resultIdsByType, resultOption) => resultOption should be(None)
    }
  }

  private def insertCount(resultIdsByType: Map[ResultOutputType, Seq[Int]]) {
    dao.insertCountResult(resultIdsByType(PATIENT_COUNT_XML).head, count, count + 42)
  }

  private def insertErrors(resultIdsByType: Map[ResultOutputType, Seq[Int]]) {
    (resultIdsByType(ERROR) zip Seq(message1, message2)).foreach {
      case (resultId, message) =>
        val pd = TestProblem.toDigest
        dao.insertErrorResult(resultId, message,pd.codec,pd.stampText,pd.summary,pd.description,pd.detailsXml)
    }
  }

  private def insertBreakdowns(resultIdsByType: Map[ResultOutputType, Seq[Int]], breakdownsByType: Map[ResultOutputType, I2b2ResultEnvelope]) {
    dao.insertBreakdownResults(resultIdsByType, breakdownsByType, breakdownsByType.mapValues(_.mapValues(_ + 42)))
  }

  private def validateCount(resultIdsByType: Map[ResultOutputType, Seq[Int]], result: ShrineQueryResult) {
    val foundCount = result.count

    foundCount.resultId should equal(resultIdsByType(PATIENT_COUNT_XML).head)
    foundCount.data.get.originalValue should equal(count)
    foundCount.data.get.obfuscatedValue should equal(count + 42)
    foundCount.creationDate should not be (null)
  }

  private def validateBreakdowns(result: ShrineQueryResult) {
    val retrievedBreakdownsByType = result.breakdowns.groupBy(_.resultType).mapValues(_.head)

    retrievedBreakdownsByType(PATIENT_AGE_COUNT_XML).resultType should equal(PATIENT_AGE_COUNT_XML)
    retrievedBreakdownsByType(PATIENT_AGE_COUNT_XML).data should equal(Map("x" -> ObfuscatedPair(1, 43), "y" -> ObfuscatedPair(2, 44)))

    retrievedBreakdownsByType(PATIENT_GENDER_COUNT_XML).resultType should equal(PATIENT_GENDER_COUNT_XML)
    retrievedBreakdownsByType(PATIENT_GENDER_COUNT_XML).data should equal(Map("a" -> ObfuscatedPair(123, 165), "b" -> ObfuscatedPair(456, 498)))
  }

  private def validateErrors(result: ShrineQueryResult) {
    val Seq(err1, err2) = result.errors

    err1.message should equal(message1)
    err2.message should equal(message2)
  }

  private def doTestFindResultsFor(response: RawCrcRunQueryResponse)(inserts: Map[ResultOutputType, Seq[Int]] => Any)(validate: (Map[ResultOutputType, Seq[Int]], Option[ShrineQueryResult]) => Any) = {
    dao.findResultsFor(networkQueryId1) should be(None)

    val queryId = dao.insertQuery(masterId1, networkQueryId1, authn, queryDef1, isFlagged = false, hasBeenRun = true, flagMessage = None)

    val resultIdsByType = dao.insertQueryResults(queryId, response.results)

    inserts(resultIdsByType)

    val resultOption = dao.findResultsFor(networkQueryId1)

    validate(resultIdsByType, resultOption)
  }
}