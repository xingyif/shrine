package net.shrine.integration

import java.net.{URL, URLConnection, URLStreamHandler, URLStreamHandlerFactory}
import java.sql.SQLException

import net.shrine.adapter.AbstractQueryRetrievalTestCase.BogusRequest
import net.shrine.adapter._
import net.shrine.adapter.client.{CouldNotParseXmlFromAdapter, HttpErrorCodeFromAdapter}
import net.shrine.adapter.components.QueryNotInDatabase
import net.shrine.adapter.dao.BotDetectedException
import net.shrine.adapter.service.{CouldNotVerifySignature, UnknownRequestType}
import net.shrine.aggregation._
import net.shrine.authentication.{NotAuthenticatedException, NotAuthenticatedProblem}
import net.shrine.authorization.{CouldNotInterpretResponseFromPmCell, CouldNotReachPmCell, ErrorStatusFromDataStewardApp, MissingRequiredRoles}
import net.shrine.broadcaster.CouldNotParseResultsException
import net.shrine.client.HttpResponse
import net.shrine.hms.authorization.HMSNotAuthenticatedProblem
import net.shrine.problem._
import net.shrine.protocol.QueryResult.StatusType
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol._
import net.shrine.qep.queries.QepDatabaseProblem
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import slick.driver.H2Driver.api._

import scala.concurrent.duration.FiniteDuration
import scala.xml.{NodeSeq, SAXParseException}

/**
  * Created by ty on 8/29/16.
  * Tests that we can successfully log every problem in the codebase.
  * Due to the time nature of logging problems, we create the succeed
  * early loop at the bottom to give every problem a chance at being
  * created on time.
  */
@RunWith(classOf[JUnitRunner])
class ProblemCreation extends FlatSpec with Matchers {
  val throwable = new IllegalArgumentException("Boo")
  val credential: Credential = Credential("string", isToken = true)
  val authInfo = AuthenticationInfo("domain", "username", credential)
  val authExecption = AdapterLockoutException(authInfo, "url")
  val bogus: ShrineRequest = new BogusRequest
  val seconds = new FiniteDuration(10, java.util.concurrent.TimeUnit.SECONDS)
  val queryDefinition = QueryDefinition("string", None)
  val runQueryRequest = new RunQueryRequest("id", seconds, authInfo, 10, None, None, Set(), queryDefinition)
  val saxxException: SAXParseException = new SAXParseException("hey", null)
  val xmlResponse: String = "<xmlResponse></xmlResponse>"
  val someXml = <details>Heyo!</details>
  val teapot: HttpResponse = HttpResponse(418, "body")
  val nodeId: NodeId = NodeId("name")
  val couldNotParseException: CouldNotParseResultsException = CouldNotParseResultsException(5, "url", "body", throwable)
  val queryResult = QueryResult(5l, 5l, None, 5l, None, None, None, StatusType("name", isDone=false), None)
  val readyQueryResponse = ReadQueryResultResponse(5l, queryResult)
  val foo: NonI2b2ableResponse = new Foo()

  "Problems" should "all be successfully created and logged" in {

    URL.setURLStreamHandlerFactory(new BogusUrlFactory)
    val db = Problems.DatabaseConnector
    val queries = Problems.Queries
    val problemSize = () => db.runBlocking(queries.size.result)

    problemSize() shouldBe 0


    val problems: Seq[AbstractProblem] = Seq(
      HttpErrorCodeFromAdapter("url", 5, "string response body"),
      CouldNotParseXmlFromAdapter("url", 6, "responseBody", saxxException),
      QueryNotFound(10l),
      QueryResultNotAvailable(10l),
      CouldNotRetrieveQueryFromCrc(10l, throwable),
      AdapterLockout(authInfo, authExecption),
      CrcCouldNotBeInvoked("crcUrl", bogus, CrcInvocationException("url", bogus, throwable)),
      AdapterMappingProblem(AdapterMappingException(runQueryRequest, "message", throwable)),
      AdapterDatabaseProblem(new SQLException("reason", "state", 5)),
      BotDetected(BotDetectedException("domain", "user", 5l, 5l, 5l)),
      CannotParseXmlFromCrc(saxxException, xmlResponse),
      ExceptionWhileLoadingCrcResponse(throwable, xmlResponse),
      ErrorFromCrcBreakdown(ErrorFromCrcException("message")),
      CannotInterpretCrcBreakdownXml(MissingCrCXmlResultException(someXml, throwable)),
      QueryNotInDatabase(I2b2AdminReadQueryDefinitionRequest("project", seconds, authInfo, 5l)),
      // Difficult to test, as I would have to pull it out of the defining code,
      // Change it from an object to a case class, and make sure that I'm not breaking
      // Any breakdown logic by doing so.
      // BreakdownFailure,
      CouldNotVerifySignature(BroadcastMessage(5l, authInfo, bogus)),
      UnknownRequestType(RequestType("apple", None)),
      NotAuthenticatedProblem(NotAuthenticatedException("string", "string", "message", throwable)),
      MissingRequiredRoles("pid", Set(), authInfo),
      CouldNotReachPmCell("url", authInfo, throwable),
      CouldNotInterpretResponseFromPmCell("url", authInfo, teapot, throwable),
      ErrorStatusFromDataStewardApp(spray.http.HttpResponse(), new URL("bogus", "host", 5, "file")),
      CouldNotConnectToAdapter(nodeId, throwable),
      TimedOutWithAdapter(nodeId),
      CouldNotParseResultsProblem(couldNotParseException),
      HttpErrorResponseProblem(couldNotParseException),
      NoValidResponsesToAggregate(),
      // Difficult to test since I can't grab private value:
      // InvalidResultProblem(Invalid(None, "error")),
      HMSNotAuthenticatedProblem(authInfo),
      ErrorStatusFromCrc(None, "<xml></xml>"),
      QepDatabaseProblem(throwable),
      ProblemNotYetEncoded("summary", Some(throwable)),
      NoI2b2AnalogExists(foo.getClass),
      TestProblem()

    )

    var count = 0
    // give it up to 1 second to finish
    while(problemSize() != problems.length && count < 20) {
      Thread.sleep(50)
      count+=1
    }

    problemSize() shouldBe problems.length
    db.runBlocking(queries.result) should contain theSameElementsAs problems.map(_.toDigest)


    }

}

class Foo extends ShrineResponse with NonI2b2ableResponse {
  override def toXml: NodeSeq = <details>Yay</details>
}

class BogusUrlFactory extends URLStreamHandlerFactory {
  override def createURLStreamHandler(protocol: String): URLStreamHandler = new BogusUrlHandler
}

class BogusUrlHandler extends URLStreamHandler {
  override def openConnection(u: URL): URLConnection = new BogusUrlConnection(u)
}

class BogusUrlConnection(u: URL) extends URLConnection(u) {
  override def connect(): Unit = {}
}