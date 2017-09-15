package net.shrine.metadata

import akka.actor.ActorRefFactory
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.{Credential, QueryResult, ResultOutputType}
import net.shrine.qep.querydb.{QepQuery, QepQueryDb, QepQueryDbChangeNotifier, QueryResultRow}
import org.json4s.DefaultFormats
import org.scalatest.{BeforeAndAfterEach, Suite}

import scala.language.postfixOps
//import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.testkit.ScalatestRouteTest

/**
  * @author david 
  * @since 3/30/17
  */
@RunWith(classOf[JUnitRunner])
class QepServiceTest extends FlatSpec with ScalatestRouteTest with QepService with TestWithDatabase {
  override def actorRefFactory: ActorRefFactory = system

  import scala.concurrent.duration._
  implicit val routeTestTimeout = RouteTestTimeout(30.seconds)
  import spray.http.StatusCodes._

  "QepService" should "return an OK and qepInfo for a dead-end route" in {
    Get(s"/qep") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(OK)(status)
      assertResult(qepInfo)(result)
    }
  }

  "QepService" should "return a NotFound and qepInfo for a non-existant route" in {
    Get(s"/qep/flarg") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(NotFound)(status)
      assertResult(qepInfo)(result)
    }
  }

  val queryReceiveTime = System.currentTimeMillis()

  val qepQuery = QepQuery(
    networkId = 1L,
    userName = "ben",
    userDomain = "testDomain",
    queryName = "testQuery",
    expression = Some("testExpression"),
    dateCreated = queryReceiveTime,
    deleted = false,
    queryXml = "testXML",
    changeDate = queryReceiveTime
  )

  val qepResultRowFromMgh = QueryResultRow(
    resultId = 10L,
    networkQueryId = 1L,
    instanceId = 100L,
    adapterNode = "MGH",
    resultType = Some(ResultOutputType.PATIENT_COUNT_XML),
    size = 30L,
    startDate = Some(queryReceiveTime + 10),
    endDate = Some(queryReceiveTime + 20),
    status = QueryResult.StatusType.Finished,
    statusMessage = None,
    changeDate = queryReceiveTime + 20
  )

  val qepResultRowFromPartners = QueryResultRow(
    resultId = 10L,
    networkQueryId = 1L,
    instanceId = 100L,
    adapterNode = "Partners",
    resultType = Some(ResultOutputType.PATIENT_COUNT_XML),
    size = 300L,
    startDate = Some(queryReceiveTime + 10),
    endDate = Some(queryReceiveTime + 20),
    status = QueryResult.StatusType.Finished,
    statusMessage = None,
    changeDate = queryReceiveTime + 30
  )

  val qepResultRowFromBch = QueryResultRow(
    resultId = 10L,
    networkQueryId = 1L,
    instanceId = 100L,
    adapterNode = "BCH",
    resultType = Some(ResultOutputType.PATIENT_COUNT_XML),
    size = 3000L,
    startDate = Some(queryReceiveTime + 10),
    endDate = Some(queryReceiveTime + 20),
    status = QueryResult.StatusType.Finished,
    statusMessage = None,
    changeDate = queryReceiveTime + 40
  )

  val qepResultRowFromDfci = QueryResultRow(
    resultId = 10L,
    networkQueryId = 1L,
    instanceId = 100L,
    adapterNode = "DFCI",
    resultType = Some(ResultOutputType.PATIENT_COUNT_XML),
    size = 30000L,
    startDate = Some(queryReceiveTime + 10),
    endDate = Some(queryReceiveTime + 20),
    status = QueryResult.StatusType.Finished,
    statusMessage = None,
    changeDate = queryReceiveTime + 50
  )

  "QepService" should "return an OK and a row of data for a queryResult request" in {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromBch)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromMgh)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromDfci)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromPartners)

    Get(s"/qep/queryResult/${qepQuery.networkId}") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(OK)(status)

      //todo check json result after format is pinned down assertResult(qepInfo)(result)
    }
  }

  "QepService" should
    """return an OK and a row of data for a queryResult request with the version and timeoutSeconds
      |parameters""".stripMargin in {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromBch)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromMgh)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromDfci)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromPartners)

    Get(s"/qep/queryResult/${qepQuery.networkId}?timeoutSeconds=10&afterVersion=${queryReceiveTime - 60}") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(OK)(status)

      //todo check json result after format is pinned down assertResult(qepInfo)(result)
    }
  }

  "QepService" should "return a NotFound with a bad query id for a queryResult request" in {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromBch)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromMgh)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromDfci)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromPartners)

    Get(s"/qep/queryResult/${20L}") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(NotFound)(status)
    }
  }

  "QepService" should """return a Forbidden if the user is the wrong user for the query for a queryResult request""" in {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromBch)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromMgh)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromDfci)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromPartners)

    Get(s"/qep/queryResult/${qepQuery.networkId}") ~> qepRoute(wrongUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(Forbidden)(status)
    }
  }

  "QepService" should
    """return an OK and a row of data for a queryResult request with the version and timeoutSeconds
      |parameters if the version hasn't changed, but not until after timeout""".stripMargin in {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromBch)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromMgh)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromDfci)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromPartners)

    val start = System.currentTimeMillis()
    val timeout = 5

    Get(s"/qep/queryResult/${qepQuery.networkId}?timeoutSeconds=$timeout&afterVersion=${queryReceiveTime+50}") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(OK)(status)

      val end = System.currentTimeMillis()

      assert(end - start >= timeout * 1000,s"The call took ${end - start} but should have taken at least ${timeout*1000}")

      //todo check json result after format is pinned down assertResult(qepInfo)(result)
    }
  }

  "QepService" should
    """return an OK and a row of data for a queryResult request with the version and timeoutSeconds
      |parameters before the timeoutSeconds if the version changes while waiting""".stripMargin in {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromMgh)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromPartners)

    val start = System.currentTimeMillis()

    val delay = 2000
    object Inserter extends Runnable {
      override def run(): Unit = {
        QepQueryDb.db.insertQepResultRow(qepResultRowFromBch)
        QepQueryDbChangeNotifier.triggerDataChangeFor(qepResultRowFromBch.networkQueryId)
      }
    }

    system.scheduler.scheduleOnce(delay milliseconds,Inserter)

    val timeout = 5

    Get(s"/qep/queryResult/${qepQuery.networkId}?timeoutSeconds=$timeout&afterVersion=${queryReceiveTime+35}") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(OK)(status)

      val end = System.currentTimeMillis()

      assert(end - start >= delay,s"The call took ${end - start} but should have taken at least $delay")
      assert(end - start < timeout * 1000,s"The call took ${end - start} but should have taken less than $timeout")

      //todo check json result after format is pinned down assertResult(qepInfo)(result)
    }
  }

  "QepService" should "return an OK and a table of data for a queryResultsTable request" in {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromBch)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromMgh)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromDfci)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromPartners)

    Get(s"/qep/queryResultsTable") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(OK)(status)

/*
      println("************")
      println(result)
      println("************")
*/
      //todo check json result      assertResult(qepInfo)(result)
    }
  }
/* todo
  "QepService" should "return an OK and a table of data for a queryResultsTable request with different skip and limit values" in {
    Get(s"/qep/queryResultsTable?skip=2&limit=2") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(OK)(status)
      //todo check json result      assertResult(qepInfo)(result)
    }
  }
*/

  val researcherUserName = "ben"
  val researcherFullName = researcherUserName

  val researcherUser = User(
    fullName = researcherUserName,
    username = researcherFullName,
    domain = "testDomain",
    credential = new Credential("researcher's password",false),
    params = Map(),
    rolesByProject = Map()
  )

  val wrongUser = User(
    fullName = "Wrong User",
    username = "wrong",
    domain = "testDomain",
    credential = new Credential("researcher's password",false),
    params = Map(),
    rolesByProject = Map()
  )

}

trait TestWithDatabase extends BeforeAndAfterEach {
  this: Suite =>

  override def beforeEach() = {
    QepQueryDb.db.createTables()
  }

  override def afterEach() = {
    QepQueryDb.db.dropTables()
  }
}
