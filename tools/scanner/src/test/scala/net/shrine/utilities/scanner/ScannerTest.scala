package net.shrine.utilities.scanner

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.client.ShrineClient
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.AggregatedRunQueryResponse
import net.shrine.protocol.AggregatedReadQueryResultResponse
import net.shrine.protocol.QueryResult
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import scala.util.Random
import net.shrine.util.XmlDateHelper
import net.shrine.config.mappings.AdapterMappings
import net.shrine.config.mappings.AdapterMappingsSource
import net.shrine.ont.data.OntologyDao
import net.shrine.ont.messaging.Concept
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.Try
import scala.util.Success
import net.shrine.authentication.Authenticator
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential

/**
 * @author clint
 * @since Mar 7, 2013
 */
final class ScannerTest extends ShouldMatchersForJUnit {
  import ScannerTest._

  private val mappings = Map("network1" -> Set("local1"), "network2" -> Set("local2", "local3"))

  private val terms = Set("foo", "bar", "baz")

  import ScannerTest.authn
  import scala.concurrent.duration._
  import SingleThreadExecutionContext.Implicits._

  private def doClientBroadcastFlagTest(scanner: Scanner, expected: Boolean) {
    scanner.client.asInstanceOf[ShrineApiScannerClient].shrineClient.asInstanceOf[HasShouldBroadcastFlag].everToldToBroadcast should be(false)
  }
  
  private def scannerClient(shrineClient: ShrineClient, authenticator: Authenticator = Authenticators.alwaysWorks): ScannerClient = {
    new ShrineApiScannerClient(shrineClient, authenticator, authn)
  }
  
  @Test
  def testScanAuthenticationFails {
    val scanner = new Scanner(
      1.day,
      0.seconds,
      literalAdapterMappingsSource(mappings),
      literalOntologyDao(terms),
      scannerClient(AllQueriesCompleteShrineClient, Authenticators.neverWorks))

    val scanResults = scanner.scan()

    doClientBroadcastFlagTest(scanner, false)

    scanResults should not be (null)
    scanResults.failed should equal(terms ++ mappings.keySet)
    scanResults.neverFinished should be(Set.empty)
    scanResults.shouldNotHaveBeenMapped should be(Set.empty)
    scanResults.shouldHaveBeenMapped should equal(Set.empty)
  }
  
  @Test
  def testScanNoErrorsNoRescanning {
    val scanner = new Scanner(
      1.day,
      0.seconds,
      literalAdapterMappingsSource(mappings),
      literalOntologyDao(terms),
      scannerClient(AllQueriesCompleteShrineClient))

    val scanResults = scanner.scan()

    doClientBroadcastFlagTest(scanner, false)

    scanResults should not be (null)
    scanResults.neverFinished should equal(Set.empty)
    scanResults.shouldNotHaveBeenMapped should be(terms)
    scanResults.shouldHaveBeenMapped should equal(Set.empty)
  }

  @Test
  def testScanOnErrorShouldForgeOnward {
    val badTerm = "blarg"

    val scanner = new Scanner(
      1.day,
      0.seconds,
      literalAdapterMappingsSource(mappings),
      literalOntologyDao(badTerm +: terms.toSeq),
      new SomeQueriesThrowScannerClient(Set(badTerm)))

    val scanResults = scanner.scan()

    doClientBroadcastFlagTest(scanner, false)

    scanResults should not be (null)
    scanResults.neverFinished should equal(Set.empty)
    scanResults.shouldNotHaveBeenMapped should be(terms)
    scanResults.shouldHaveBeenMapped should equal(Set.empty)
    scanResults.failed should equal(Set(badTerm))
  }

  @Test
  def testScanAllErrorsNoRescanning {
    val scanner = new Scanner(
      1.day,
      0.seconds,
      literalAdapterMappingsSource(mappings),
      literalOntologyDao(terms),
      scannerClient(AllQueriesErrorShrineClient))

    val scanResults = scanner.scan()

    doClientBroadcastFlagTest(scanner, false)

    scanResults should not be (null)
    scanResults.neverFinished should equal(Set.empty)
    scanResults.shouldNotHaveBeenMapped should equal(Set.empty)
    scanResults.shouldHaveBeenMapped should be(mappings.keySet)
    scanResults.failed should equal(Set.empty)
  }

  @Test
  def testScanSomeProblemsNoRescanning {
    val scanner = new Scanner(
      1.day,
      0.seconds,
      literalAdapterMappingsSource(mappings),
      literalOntologyDao(terms),
      scannerClient(someQueriesWorkShrineClient(Set("network1", "foo"), Set("network2", "bar", "baz"), Set.empty)))

    val scanResults = scanner.scan()

    doClientBroadcastFlagTest(scanner, false)

    scanResults should not be (null)
    scanResults.neverFinished should equal(Set.empty)
    scanResults.shouldNotHaveBeenMapped should be(Set("foo"))
    scanResults.shouldHaveBeenMapped should be(Set("network2"))
    scanResults.failed should equal(Set.empty)
  }

  @Test
  def testScanSomeProblemsRescanningSucceeds {
    val scanner = new Scanner(
      1.day,
      0.seconds,
      literalAdapterMappingsSource(mappings),
      literalOntologyDao(terms),
      scannerClient(someQueriesWorkShrineClient(Set.empty, Set("network2", "bar", "baz"), Set.empty, Set("network1", "foo"))))

    val scanResults = scanner.scan()

    doClientBroadcastFlagTest(scanner, false)

    scanResults should not be (null)
    scanResults.neverFinished should equal(Set.empty)
    scanResults.shouldNotHaveBeenMapped should be(Set("foo"))
    scanResults.shouldHaveBeenMapped should be(Set("network2"))
    scanResults.failed should equal(Set.empty)
  }

  @Test
  def testScanSomeProblemsRescanningDoesntGetResults {
    val scanner = new Scanner(
      1.day,
      0.seconds,
      literalAdapterMappingsSource(mappings),
      literalOntologyDao(terms),
      scannerClient(someQueriesWorkShrineClient(Set("bar"), Set("network2", "baz"), Set("network1", "foo"))))

    val scanResults = scanner.scan()

    doClientBroadcastFlagTest(scanner, false)

    scanResults should not be (null)
    scanResults.neverFinished should be(Set("network1", "foo"))
    scanResults.shouldNotHaveBeenMapped should be(Set("bar"))
    scanResults.shouldHaveBeenMapped should be(Set("network2"))
    scanResults.failed should equal(Set.empty)
  }
}

object ScannerTest {
  private val random = new Random
  
  private val authn = AuthenticationInfo("d", "u", Credential("p", false))

  private def literalOntologyDao(terms: Iterable[String]): OntologyDao = new OntologyDao {
    override val ontologyEntries = terms.map(t => Concept(t, None, None))
  }

  private def literalAdapterMappingsSource(mappings: Map[String, Set[String]]): AdapterMappingsSource = new AdapterMappingsSource {
    override def load(source:String): Try[AdapterMappings] = Success(AdapterMappings("scannerTest",mappings = mappings))
    override def lastModified: Long = 0l
  }

  import QueryResult.StatusType

  private object AllQueriesCompleteShrineClient extends ShrineClientAdapter with HasShouldBroadcastFlag {
    override def runQuery(topicId: String, outputTypes: Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse = {
      this.everToldToBroadcast ||= shouldBroadcast

      aggregatedRunQueryResponse(random.nextLong, queryDefinition, StatusType.Finished)
    }
  }

  private object AllQueriesErrorShrineClient extends ShrineClientAdapter with HasShouldBroadcastFlag {
    override def runQuery(topicId: String, outputTypes: Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse = {
      this.everToldToBroadcast ||= shouldBroadcast

      aggregatedRunQueryResponse(random.nextLong, queryDefinition, StatusType.Error)
    }
  }

  import SingleThreadExecutionContext.Implicits._
  
  private final class SomeQueriesThrowScannerClient(badTerms: Set[String]) extends ShrineApiScannerClient(AllQueriesCompleteShrineClient, Authenticators.alwaysWorks, authn) {
    import Scanner.Termable

    private def possiblyThrow[T: Termable](f: T => Future[TermResult]): T => Future[TermResult] = {
      input =>
        val term = implicitly[Termable[T]].getTerm(input)

        if (badTerms.contains(term)) {
          throw new Exception("blarg") with scala.util.control.NoStackTrace
        } else {
          f(input)
        }
    }

    override def query(term: String): Future[TermResult] = {
      val q = possiblyThrow(super.query)

      q(term)
    }

    override def retrieveResults(termResult: TermResult): Future[TermResult] = {
      val r = possiblyThrow(super.retrieveResults)

      r(termResult)
    }
  }

  private def someQueriesWorkShrineClient(termsThatShouldWork: Set[String], termsThatShouldNotWork: Set[String], termsThatShouldNeverFinish: Set[String], termsThatShouldFinishAfter1Retry: Set[String] = Set.empty): ShrineClient = new ShrineClientAdapter with HasShouldBroadcastFlag {
    final case class Query(networkQueryId: Long, term: String)
    
    var timedOutTerms = Map.empty[Long, Query]

    override def runQuery(topicId: String, outputTypes: Set[ResultOutputType], queryDefinition: QueryDefinition, shouldBroadcast: Boolean): AggregatedRunQueryResponse = {
      this.everToldToBroadcast ||= shouldBroadcast

      val Term(term) = queryDefinition.expr.get

      val queryId = random.nextLong

      val statusType = if (termsThatShouldWork.contains(term)) { StatusType.Finished }
                       else if (termsThatShouldNotWork.contains(term)) { StatusType.Error }
                       else { StatusType.Processing }

      val aggregatedResponse = aggregatedRunQueryResponse(queryId, queryDefinition, statusType)

      if (termsThatShouldFinishAfter1Retry.contains(term)) {
        timedOutTerms += (aggregatedResponse.results.head.resultId -> Query(queryId, term))
      }

      aggregatedResponse
    }

    override def readQueryResult(networkResultId: Long, shouldBroadcast: Boolean): AggregatedReadQueryResultResponse = {
      this.everToldToBroadcast ||= shouldBroadcast

      val (status, queryId) = timedOutTerms.get(networkResultId) match {
        case Some(Query(networkQueryId, _)) => (StatusType.Finished, networkQueryId) 
        case None => (StatusType.Processing, -1L)
      }

      AggregatedReadQueryResultResponse(queryId, Seq(queryResult(status)))
    }
  }

  private trait HasShouldBroadcastFlag {
    var everToldToBroadcast = false
  }

  private def queryResult(status: QueryResult.StatusType): QueryResult = {
    val resultType = if (status.isError) None else Some(ResultOutputType.PATIENT_COUNT_XML)

    val resultId = random.nextLong
    val instanceId = random.nextLong

    QueryResult(resultId, instanceId, resultType, 99, Some(XmlDateHelper.now), Some(XmlDateHelper.now), None, status, None)
  }

  private def aggregatedRunQueryResponse(queryId: Long, queryDefinition: QueryDefinition, status: QueryResult.StatusType): AggregatedRunQueryResponse = {
    AggregatedRunQueryResponse(queryId, XmlDateHelper.now, "some-userId", "some-groupId", queryDefinition, random.nextLong, Seq(queryResult(status)))
  }
}
