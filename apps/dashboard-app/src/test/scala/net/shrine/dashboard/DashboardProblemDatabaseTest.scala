package net.shrine.dashboard
import net.shrine.problem.ProblemDigest
import slick.driver.H2Driver.api._
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory sqlite3 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
class DashboardProblemDatabaseTest extends FlatSpec with BeforeAndAfter with ScalaFutures with Matchers {
  implicit val timeout = 10.seconds

  val connector = ProblemDatabaseConnector(DBUrls.H2Mem)
  val problemDigests = Seq(
    // Not actually sure what examples of ProblemDigests look like
    ProblemDigest("MJPG", "01:01:01", "summary here", "description here"     , <details>uh not sure</details>),
    ProblemDigest("wewu", "01:02:01", "coffee spill", "coffee everywhere"    , <details>He chose decaf</details>),
    ProblemDigest("wuwu", "02:01:01", "squirrel"    , "chewed all the cables", <details>Like ALL of them</details>),
    ProblemDigest("code", "10:01:02", "such summary", "such description"     , <details>Wow</details>))

  before {
    connector.runBlocking(connector.tableExists) shouldBe false
    connector.runBlocking(connector.createIfNotExists >> connector.tableExists) shouldBe true
    connector.runBlocking(connector.createIfNotExists) shouldBe NoOperation
    connector.runBlocking(connector.selectAll) shouldBe empty
  }

  after {
    connector.runBlocking(connector.tableExists) shouldBe true
    connector.runBlocking(connector.dropIfExists >> connector.tableExists) shouldBe false
    connector.runBlocking(connector.dropIfExists) shouldBe NoOperation
  }

  "The Database" should "Connect without any problems" in {
    // Insert the suppliers and ProblemDigests
    connector.executeTransactionBlocking(connector.problems ++= problemDigests)

    // Test that they are all in the table
    var * = connector.runBlocking(connector.selectAll)
    * should contain theSameElementsAs problemDigests
    * should have length problemDigests.length

    // Reset the table
    connector.runBlocking(connector.resetTable >> connector.selectAll) shouldBe empty

    // Run the test again
    connector.executeTransactionBlocking(connector.problems += problemDigests.head,
                                         connector.problems += problemDigests(1),
                                         connector.problems += problemDigests(2),
                                         connector.problems += problemDigests(3))
    // Test that they are all in the table
    * = connector.runBlocking(connector.selectAll)
    * should contain theSameElementsAs problemDigests
    * should have length problemDigests.length


    // Test that the simple select and filter queries work
    val filtered = connector.runBlocking(connector.problems.filter(_.codec === "code").map(_.description).result)
    filtered should have length 1
    filtered.head shouldBe problemDigests(3).description

    // This also tests that our conversion from xml to strings works
    val xml = connector.runBlocking(connector.problems.map(_.xml).result)
    xml should have length problemDigests.length
    xml should contain theSameElementsAs problemDigests.map(_.detailsXml.toString())
  }
}