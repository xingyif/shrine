package net.shrine.adapter.dao.model

import net.shrine.problem.TestProblem
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.QueryResult.StatusType
import net.shrine.util.XmlDateHelper
import net.shrine.protocol.QueryResult
import net.shrine.protocol.I2b2ResultEnvelope
import net.shrine.protocol.query.{QueryDefinition, Term}
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 * @author clint
 * @since Nov 1, 2012
 */
final class ShrineQueryResultTest extends ShouldMatchersForJUnit {
  import ResultOutputType._
  import DefaultBreakdownResultOutputTypes._

  private val queryId = 999

  private def someId = scala.util.Random.nextInt

  private def queryResultRow(resultType: ResultOutputType) = QueryResultRow(
    someId,
    someId.toLong,
    queryId,
    resultType,
    (if (resultType.isError) StatusType.Error else StatusType.Finished),
    (if (resultType.isError) None else Some(100)),
    XmlDateHelper.now)

  private val countQueryResultRow = queryResultRow(PATIENT_COUNT_XML)

  private val breakdownQueryResultRow1 = queryResultRow(PATIENT_AGE_COUNT_XML)

  private val breakdownQueryResultRow2 = queryResultRow(PATIENT_GENDER_COUNT_XML)

  private val errorQueryResultRow1 = queryResultRow(ERROR)

  private val errorQueryResultRow2 = queryResultRow(ERROR)

  private val topicId = "some-topic-id"
  
  private val flagMessage = Some("alksjdkalsjdalsjd")

  private val queryName = "some-query"
  private val queryExpr = Term("foo")
  private val queryDefinition = QueryDefinition(queryName,queryExpr)
  private val queryRow = ShrineQuery(123, "48573498739845", 392874L, queryName, "some-user", "some-domain", XmlDateHelper.now, isFlagged = true, flagMessage = flagMessage,queryDefinition = queryDefinition)
  
  private val resultRows = Seq(countQueryResultRow, breakdownQueryResultRow1, breakdownQueryResultRow2, errorQueryResultRow1, errorQueryResultRow2)

  private val countRow = CountRow(someId, countQueryResultRow.id, 99, 100, XmlDateHelper.now)

  private val breakdownRows = Map(
    PATIENT_AGE_COUNT_XML -> Seq(BreakdownResultRow(someId, breakdownQueryResultRow1.id, "x", 1, 2), BreakdownResultRow(someId, breakdownQueryResultRow1.id, "y", 2, 3)),
    PATIENT_GENDER_COUNT_XML -> Seq(BreakdownResultRow(someId, breakdownQueryResultRow2.id, "a", 9, 10), BreakdownResultRow(someId, breakdownQueryResultRow2.id, "b", 10, 11)))

  private val pd = TestProblem.toDigest

  private val errorRows = Seq(ShrineError(someId, errorQueryResultRow1.id, "foo", pd.codec,pd.stampText,pd.summary,pd.description,pd.detailsXml), ShrineError(someId, errorQueryResultRow2.id, "bar", pd.codec,pd.stampText,pd.summary,pd.description,pd.detailsXml))

  @Test
  def testToQueryResults {
    val Some(shrineQueryResult) = ShrineQueryResult.fromRows(queryRow, resultRows, countRow, breakdownRows, errorRows)

    for (doObfuscation <- Seq(true, false)) {
      def obfuscate(i: Int) = if (doObfuscation) i + 1 else i

      val expected = Count.fromRows(countQueryResultRow, countRow).toQueryResult.map(_.withBreakdowns(Map(
        PATIENT_AGE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_AGE_COUNT_XML, Map("x" -> obfuscate(1), "y" -> obfuscate(2))),
        PATIENT_GENDER_COUNT_XML -> I2b2ResultEnvelope(PATIENT_GENDER_COUNT_XML, Map("a" -> obfuscate(9), "b" -> obfuscate(10))))))

      shrineQueryResult.toQueryResults(doObfuscation) should equal(expected)
    }
  }

  @Test
  def testFromRows {
    ShrineQueryResult.fromRows(null, Nil, countRow, breakdownRows, errorRows) should be(None)

    val Some(shrineQueryResult) = ShrineQueryResult.fromRows(queryRow, resultRows, countRow, breakdownRows, errorRows)

    shrineQueryResult.isFlagged should be(queryRow.isFlagged)
    shrineQueryResult.flagMessage should be(flagMessage)

    shrineQueryResult.count should equal(Count.fromRows(countQueryResultRow, countRow))
    shrineQueryResult.errors should equal(errorRows)
    shrineQueryResult.breakdowns.toSet should equal(Set(
   		Breakdown(breakdownQueryResultRow1.id, breakdownQueryResultRow1.localId, PATIENT_AGE_COUNT_XML, Map("x" -> ObfuscatedPair(1, 2), "y" -> ObfuscatedPair(2, 3))),
   		Breakdown(breakdownQueryResultRow2.id, breakdownQueryResultRow2.localId, PATIENT_GENDER_COUNT_XML, Map("a" -> ObfuscatedPair(9, 10), "b" -> ObfuscatedPair(10, 11)))))
  }
}