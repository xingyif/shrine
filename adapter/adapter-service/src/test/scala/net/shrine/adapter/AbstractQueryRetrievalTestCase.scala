package net.shrine.adapter

import scala.xml.NodeSeq
import net.shrine.util.ShouldMatchersForJUnit
import ObfuscatorTest.within3
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.adapter.dao.AdapterDao
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.client.HttpClient
import net.shrine.client.HttpResponse
import net.shrine.protocol.{HiveCredentials, AuthenticationInfo, BroadcastMessage, CrcRequest, Credential, ErrorResponse, I2b2ResultEnvelope, QueryResult, ReadResultRequest, ReadResultResponse, ResultOutputType, ShrineRequest, ShrineResponse, BaseShrineResponse, BaseShrineRequest, RunQueryRequest, RunQueryResponse, DefaultBreakdownResultOutputTypes}
import net.shrine.protocol.DefaultBreakdownResultOutputTypes.PATIENT_AGE_COUNT_XML
import net.shrine.protocol.ResultOutputType.PATIENT_COUNT_XML
import net.shrine.protocol.DefaultBreakdownResultOutputTypes.PATIENT_GENDER_COUNT_XML
import net.shrine.protocol.query.{QueryDefinition, Term}
import net.shrine.util.XmlDateHelper
import net.shrine.util.XmlDateHelper.now
import net.shrine.util.XmlGcEnrichments
import net.shrine.client.Poster
import net.shrine.adapter.translators.QueryDefinitionTranslator
import net.shrine.adapter.translators.ExpressionTranslator
import scala.util.Success

/**
 * @author clint
 * @since Nov 8, 2012
 */
//noinspection UnitMethodIsParameterless
abstract class AbstractQueryRetrievalTestCase[R <: BaseShrineResponse](
  makeAdapter: (AdapterDao, HttpClient) => WithHiveCredentialsAdapter,
  makeRequest: (Long, AuthenticationInfo) => BaseShrineRequest,
  extractor: R => Option[(Long, QueryResult)]) extends AbstractSquerylAdapterTest with ShouldMatchersForJUnit {

  private val authn = AuthenticationInfo("some-domain", "some-user", Credential("alskdjlkasd", false))

  def doTestProcessRequestMissingQuery {
    val adapter = makeAdapter(dao, MockHttpClient)

    val response = adapter.processRequest(BroadcastMessage(0L, authn, makeRequest(-1L, authn)))

    response.isInstanceOf[ErrorResponse] should be(true)
  }

  def doTestProcessInvalidRequest {
    val adapter = makeAdapter(dao, MockHttpClient)

    intercept[ClassCastException] {
      //request must be a type of request we can handle
      adapter.processRequest(BroadcastMessage(0L, authn, new AbstractQueryRetrievalTestCase.BogusRequest))
    }
  }

  private val localMasterId = "alksjdkalsdjlasdjlkjsad"

  private val shrineNetworkQueryId = 123L

  private def doGetResults(adapter: Adapter) = adapter.processRequest(BroadcastMessage(shrineNetworkQueryId, authn, makeRequest(shrineNetworkQueryId, authn)))

  private def toMillis(xmlGc: XMLGregorianCalendar): Long = xmlGc.toGregorianCalendar.getTimeInMillis

  private val instanceId = 999L
  private val setSize = 12345L
  private val obfSetSize = setSize + 1
  private val queryExpr = Term("foo")
  private val topicId = "laskdjlkasd"
  private val fooQuery = QueryDefinition("some-query",queryExpr)


  def doTestProcessRequestIncompleteQuery(countQueryShouldWork: Boolean = true): Unit = afterCreatingTables {

    val dbQueryId = dao.insertQuery(localMasterId, shrineNetworkQueryId, authn, fooQuery, isFlagged = false, hasBeenRun = true, flagMessage = None)

    import ResultOutputType._
    import XmlDateHelper.now

    val breakdowns = Map(PATIENT_AGE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_AGE_COUNT_XML, Map("a" -> 1L, "b" -> 2L)))

    val obfscBreakdowns = breakdowns.mapValues(_.mapValues(_ + 1))

    val startDate = now
    val elapsed = 100L

    val endDate = {
      import XmlGcEnrichments._
      import scala.concurrent.duration._

      startDate + elapsed.milliseconds
    }

    val countResultId = 456L
    val breakdownResultId = 98237943265436L

    val incompleteCountResult = QueryResult(
      resultId = countResultId,
      instanceId = instanceId,
      resultType = Some(PATIENT_COUNT_XML),
      setSize = setSize,
      startDate = Option(startDate),
      endDate = Option(endDate),
      description = Some("results from node X"),
      statusType = QueryResult.StatusType.Processing,
      statusMessage = None,
      breakdowns = breakdowns)

    val breakdownResult = breakdowns.head match {
      case (resultType, data) => incompleteCountResult.withId(breakdownResultId).withBreakdowns(Map(resultType -> data)).withResultType(resultType)
    }

    val queryStartDate = now

    val idsByResultType = dao.insertQueryResults(dbQueryId, incompleteCountResult :: breakdownResult :: Nil)

    final class MightWorkMockHttpClient(expectedHiveCredentials: HiveCredentials) extends HttpClient {
      override def post(input: String, url: String): HttpResponse = {
        def makeFinished(queryResult: QueryResult) = queryResult.copy(statusType = QueryResult.StatusType.Finished)

        def validateAuthnAndProjectId(req: ShrineRequest) {
          req.authn should equal(expectedHiveCredentials.toAuthenticationInfo)

          req.projectId should equal(expectedHiveCredentials.projectId)
        }

        val response = CrcRequest.fromI2b2String(DefaultBreakdownResultOutputTypes.toSet)(input) match {
          case Success(req: ReadResultRequest) if req.localResultId == countResultId.toString => {
            validateAuthnAndProjectId(req)

            if (countQueryShouldWork) {
              ReadResultResponse(123L, makeFinished(incompleteCountResult), I2b2ResultEnvelope(PATIENT_COUNT_XML, Map(PATIENT_COUNT_XML.name -> incompleteCountResult.setSize)))
            } else {
              ErrorResponse("Retrieving count result failed")
            }
          }
          case Success(req: ReadResultRequest) if req.localResultId == breakdownResultId.toString => {
            validateAuthnAndProjectId(req)

            ReadResultResponse(123L, makeFinished(breakdownResult), breakdowns.head._2)
          }
          case _ => fail(s"Unknown input: $input")
        }

        HttpResponse.ok(response.toI2b2String)
      }
    }

    val adapter: WithHiveCredentialsAdapter = makeAdapter(dao, new MightWorkMockHttpClient(AbstractQueryRetrievalTestCase.hiveCredentials))

    def getResults = doGetResults(adapter)

    getResults.isInstanceOf[ErrorResponse] should be(true)

    dao.insertCountResult(idsByResultType(PATIENT_COUNT_XML).head, setSize, obfSetSize)

    dao.insertBreakdownResults(idsByResultType, breakdowns, obfscBreakdowns)

    //The query shouldn't be 'done', since its status is PROCESSING
    dao.findResultsFor(shrineNetworkQueryId).get.count.statusType should be(QueryResult.StatusType.Processing)

    //Now, calling processRequest (via getResults) should cause the query to be re-retrieved from the CRC

    val result = getResults.asInstanceOf[R]

    //Which should cause the query to be re-stored with a 'done' status (since that's what our mock CRC returns)

    val expectedStatusType = if (countQueryShouldWork) QueryResult.StatusType.Finished else QueryResult.StatusType.Processing

    dao.findResultsFor(shrineNetworkQueryId).get.count.statusType should be(expectedStatusType)

    if (!countQueryShouldWork) {
      result.isInstanceOf[ErrorResponse] should be(true)
    } else {
      val Some((actualNetworkQueryId, actualQueryResult)) = extractor(result)

      actualNetworkQueryId should equal(shrineNetworkQueryId)

      import ObfuscatorTest.within3

      actualQueryResult.resultType should equal(Some(PATIENT_COUNT_XML))
      within3(setSize, actualQueryResult.setSize) should be(true)
      actualQueryResult.description should be(Some("results from node X"))
      actualQueryResult.statusType should equal(QueryResult.StatusType.Finished)
      actualQueryResult.statusMessage should be(Some(QueryResult.StatusType.Finished.name))

      actualQueryResult.breakdowns.foreach {
        case (rt, I2b2ResultEnvelope(_, data)) => {
          data.forall { case (key, value) => within3(value, breakdowns.get(rt).get.data.get(key).get) }
        }
      }

      for {
        startDate <- actualQueryResult.startDate
        endDate <- actualQueryResult.endDate
      } {
        val actualElapsed = toMillis(endDate) - toMillis(startDate)

        actualElapsed should equal(elapsed)
      }
    }
  }

  def doTestProcessRequestQueuedQuery: Unit = afterCreatingTables {

    import ResultOutputType._
    import XmlDateHelper.now
    val startDate = now
    val elapsed = 100L

    val endDate = {
      import XmlGcEnrichments._
      import scala.concurrent.duration._

      startDate + elapsed.milliseconds
    }

    val countResultId = 456L

    val incompleteCountResult = QueryResult(-1L, -1L, Some(PATIENT_COUNT_XML), -1L, Option(startDate), Option(endDate), Some("results from node X"), QueryResult.StatusType.Queued, None)

    dao.inTransaction {
      val insertedQueryId = dao.insertQuery(localMasterId, shrineNetworkQueryId, authn, fooQuery, isFlagged = false, hasBeenRun = false, flagMessage = None)

      //NB: We need to insert dummy QueryResult and Count records so that calls to StoredQueries.retrieve() in 
      //AbstractReadQueryResultAdapter, called when retrieving results for previously-queued-or-incomplete 
      //queries, will work.

      val insertedQueryResultIds = dao.insertQueryResults(insertedQueryId, Seq(incompleteCountResult))

      val countQueryResultId = insertedQueryResultIds(ResultOutputType.PATIENT_COUNT_XML).head

      dao.insertCountResult(countQueryResultId, -1L, -1L)
    }

    val queryStartDate = now

    object MockHttpClient extends HttpClient {
      override def post(input: String, url: String): HttpResponse = ???
    }

    val adapter: WithHiveCredentialsAdapter = makeAdapter(dao, MockHttpClient)

    def getResults = doGetResults(adapter)

    getResults.isInstanceOf[ErrorResponse] should be(true)

    //The query shouldn't be 'done', since its status is QUEUED
    dao.findResultsFor(shrineNetworkQueryId).get.count.statusType should be(QueryResult.StatusType.Queued)

    //Now, calling processRequest (via getResults) should NOT cause the query to be re-retrieved from the CRC, because the query was previously queued

    val result = getResults

    result.isInstanceOf[ErrorResponse] should be(true)

    dao.findResultsFor(shrineNetworkQueryId).get.count.statusType should be(QueryResult.StatusType.Queued)
  }

  def doTestProcessRequest = afterCreatingTables {

    val adapter = makeAdapter(dao, MockHttpClient)

    def getResults = doGetResults(adapter)

    getResults match {
      case errorResponse:ErrorResponse => errorResponse.problemDigest.codec should be (classOf[QueryNotFound].getName)
      case x => fail(s"Got $x, not an ErrorResponse")
    }

    val dbQueryId = dao.insertQuery(localMasterId, shrineNetworkQueryId, authn, fooQuery, isFlagged = false, hasBeenRun = false, flagMessage = None)

    getResults match {
      case errorResponse:ErrorResponse => errorResponse.problemDigest.codec should be (classOf[QueryNotFound].getName)
      case x => fail(s"Got $x, not an ErrorResponse")
    }

    import ResultOutputType._
    import XmlDateHelper.now

    val breakdowns = Map(
      PATIENT_AGE_COUNT_XML -> I2b2ResultEnvelope(PATIENT_AGE_COUNT_XML, Map("a" -> 1L, "b" -> 2L)),
      PATIENT_GENDER_COUNT_XML -> I2b2ResultEnvelope(PATIENT_GENDER_COUNT_XML, Map("x" -> 3L, "y" -> 4L)))

    val obfscBreakdowns = breakdowns.mapValues(_.mapValues(_ + 1))

    val startDate = now
    val elapsed = 100L

    val endDate = {
      import XmlGcEnrichments._
      import scala.concurrent.duration._

      startDate + elapsed.milliseconds
    }

    val countResult = QueryResult(
      resultId = 456L,
      instanceId = instanceId,
      resultType = Some(PATIENT_COUNT_XML),
      setSize = setSize,
      startDate = Option(startDate),
      endDate = Option(endDate),
      description = Some("results from node X"),
      statusType = QueryResult.StatusType.Finished,
      statusMessage = None,
      breakdowns = breakdowns
    )

    val breakdownResults = breakdowns.map {
      case (resultType, data) =>
        countResult.withBreakdowns(Map(resultType -> data)).withResultType(resultType)
    }.toSeq

    val queryStartDate = now

    val idsByResultType = dao.insertQueryResults(dbQueryId, countResult +: breakdownResults)

    getResults.isInstanceOf[ErrorResponse] should be(true)

    dao.insertCountResult(idsByResultType(PATIENT_COUNT_XML).head, setSize, obfSetSize)

    dao.insertBreakdownResults(idsByResultType, breakdowns, obfscBreakdowns)

    val result = getResults.asInstanceOf[R]

    val Some((actualNetworkQueryId, actualQueryResult)) = extractor(result)

    actualNetworkQueryId should equal(shrineNetworkQueryId)

    actualQueryResult.resultType should equal(Some(PATIENT_COUNT_XML))
    actualQueryResult.setSize should equal(obfSetSize)
    actualQueryResult.description should be(None) //TODO: This is probably wrong
    actualQueryResult.statusType should equal(QueryResult.StatusType.Finished)
    actualQueryResult.statusMessage should be(None)
    actualQueryResult.breakdowns should equal(obfscBreakdowns)

    for {
      startDate <- actualQueryResult.startDate
      endDate <- actualQueryResult.endDate
    } {
      val actualElapsed = toMillis(endDate) - toMillis(startDate)

      actualElapsed should equal(elapsed)
    }
  }
}

object AbstractQueryRetrievalTestCase {
  val hiveCredentials = HiveCredentials("some-hive-domain", "hive-username", "hive-password", "hive-project")

  val doObfuscation = true

  def runQueryAdapter(dao: AdapterDao, poster: Poster): RunQueryAdapter = {
    val translator = new QueryDefinitionTranslator(new ExpressionTranslator(Map("foo" -> Set("bar"))))

    new RunQueryAdapter(
      poster,
      dao,
      AbstractQueryRetrievalTestCase.hiveCredentials,
      translator,
      10000,
      doObfuscation,
      runQueriesImmediately = true,
      DefaultBreakdownResultOutputTypes.toSet,
      collectAdapterAudit = false
    )
  }

  import scala.concurrent.duration._

  final class BogusRequest extends ShrineRequest("fooProject", 1.second, null) {
    override val requestType = null

    protected override def i2b2MessageBody: NodeSeq = <foo></foo>

    override def toXml = <x></x>
  }
}