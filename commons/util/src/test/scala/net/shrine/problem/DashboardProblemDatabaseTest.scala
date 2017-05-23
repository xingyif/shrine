package net.shrine.problem

import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import slick.driver.H2Driver.api._ //todo do without this import

import scala.concurrent.duration._

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory H2 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
@RunWith(classOf[JUnitRunner])
class DashboardProblemDatabaseTest extends FlatSpec with BeforeAndAfter with ScalaFutures with Matchers {
  implicit val timeout = 10.seconds
  val connector = Problems.DatabaseConnector
  val IO = connector.IO
  val problemDigests = Seq(
    ProblemDigest("MJPG", "01:01:01", "summary here", "description here"     , <details>uh not sure</details>     , 2),
    ProblemDigest("wewu", "01:02:01", "coffee spill", "coffee everywhere"    , <details>He chose decaf</details>  , 1),
    ProblemDigest("wuwu", "02:01:01", "squirrel"    , "chewed all the cables", <details>Like ALL of them</details>, 0),
    ProblemDigest("code", "10:01:02", "such summary", "such description"     , <details>Wow</details>             , 3))

  before {
    connector.runBlocking(IO.dropIfExists >> IO.tableExists) shouldBe false
    connector.runBlocking(IO.createIfNotExists >> IO.tableExists) shouldBe true
    connector.runBlocking(IO.createIfNotExists) shouldBe NoOperation
    connector.runBlocking(IO.selectAll) shouldBe empty
  }

  after {
    connector.runBlocking(IO.tableExists) shouldBe true
    connector.runBlocking(IO.dropIfExists >> IO.tableExists) shouldBe false
    connector.runBlocking(IO.dropIfExists) shouldBe NoOperation
  }

  "The Database" should "Connect without any problems" in {
    // Insert the suppliers and ProblemDigests
    connector.executeTransactionBlocking(IO.problems ++= problemDigests)

    // Test that they are all in the table
    var * = connector.runBlocking(IO.selectAll)
    * should contain theSameElementsAs problemDigests
    * should have length problemDigests.length

    // Reset the table
    connector.runBlocking(IO.resetTable >> IO.selectAll) shouldBe empty

    // Run the test again
    connector.executeTransactionBlocking(IO.problems += problemDigests.head,
                                         IO.problems += problemDigests(1),
                                         IO.problems += problemDigests(2),
                                         IO.problems += problemDigests(3))
    // Test that they are all in the table
    * = connector.runBlocking(IO.selectAll)
    * should contain theSameElementsAs problemDigests
    * should have length problemDigests.length


    // Test that the simple select and filter queries work
    val filtered = connector.runBlocking(IO.problems.filter(_.codec === "code").map(_.description).result)
    filtered should have length 1
    filtered.head shouldBe problemDigests(3).description

    // This also tests that our conversion from xml to strings works
    val xml = connector.runBlocking(IO.problems.map(_.xml).result)
    xml should have length problemDigests.length
    xml should contain theSameElementsAs problemDigests.map(_.detailsXml.toString())

    val result = connector.runBlocking(IO.sizeAndProblemDigest(2))
    result._1 should have length 2
    result._2 shouldBe problemDigests.length
    result._1.head shouldBe problemDigests(3)
    result._1(1) shouldBe problemDigests.head

    val resultOverLength = connector.runBlocking(IO.sizeAndProblemDigest(10))
    resultOverLength._1 should have length 4
    resultOverLength._1 should contain theSameElementsAs problemDigests

    connector.runBlocking(IO.problems.size.result) shouldBe problemDigests.size

    val testProblem = ProblemDatabaseTestProblem(ProblemSources.Unknown)
    Thread.sleep(200)
    connector.runBlocking(IO.problems.size.result) shouldBe problemDigests.size + 1
  }
}

case class ProblemDatabaseTestProblem(source: ProblemSources.ProblemSource) extends AbstractProblem(source: ProblemSources.ProblemSource) {
  override def summary: String = "This is a test problem! No user should ever see this."
  override def description: String = "Wow, this is a nice looking problem. I mean really, just look at it."
}