package net.shrine.metadata

import akka.actor.ActorRefFactory
import net.shrine.i2b2.protocol.pm.User
import net.shrine.protocol.{Credential, QueryResult, ResultOutputType}
import net.shrine.qep.queries.{QepQuery, QepQueryDb, QueryResultRow}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.parse
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.testkit.ScalatestRouteTest

/**
  * @author david 
  * @since 3/30/17
  */
@RunWith(classOf[JUnitRunner])
class QepServiceTest extends FlatSpec with ScalatestRouteTest with QepService {
  override def actorRefFactory: ActorRefFactory = system

  import scala.concurrent.duration._
  implicit val routeTestTimeout = RouteTestTimeout(10.seconds)
  import spray.http.StatusCodes._

  "QepService" should "return an OK and qepInfo for a dead-end route" in {
    Get(s"/qep") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(OK)(status)
      assertResult(qepInfo)(result)
    }
  }

  val qepQuery = QepQuery(
    networkId = 1L,
    userName = "ben",
    userDomain = "testDomain",
    queryName = "testQuery",
    expression = Some("testExpression"),
    dateCreated = System.currentTimeMillis(),
    deleted = false,
    queryXml = "testXML",
    changeDate = System.currentTimeMillis()
  )

  val qepResultRowFromExampleCom = QueryResultRow(
    resultId = 10L,
    networkQueryId = 1L,
    instanceId = 100L,
    adapterNode = "example.com",
    resultType = Some(ResultOutputType.PATIENT_COUNT_XML),
    size = 30L,
    startDate = Some(System.currentTimeMillis() - 60),
    endDate = Some(System.currentTimeMillis() - 30),
    status = QueryResult.StatusType.Finished,
    statusMessage = None,
    changeDate = System.currentTimeMillis() - 30
  )


  "QepService" should "return an OK and a table of data for a queryResults request" in {

    QepQueryDb.db.insertQepQuery(qepQuery)
    QepQueryDb.db.insertQepResultRow(qepResultRowFromExampleCom)

    Get(s"/qep/queryResults") ~> qepRoute(researcherUser) ~> check {
      implicit val formats = DefaultFormats
      val result = body.data.asString

      assertResult(OK)(status)

      println("************")
      println(result)
      println("************")

      //todo check json result      assertResult(qepInfo)(result)
    }
  }
/* todo
  "QepService" should "return an OK and a table of data for a queryResults request with different skip and limit values" in {
    Get(s"/qep/queryResults?skip=2&limit=2") ~> qepRoute(researcherUser) ~> check {
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
}