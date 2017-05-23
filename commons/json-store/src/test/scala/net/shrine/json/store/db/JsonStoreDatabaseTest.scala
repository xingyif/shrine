package net.shrine.json.store.db

import java.util.UUID

import net.shrine.json.store.db.JsonStoreDatabase.IOActions.NoOperation
import net.shrine.json.store.db.JsonStoreDatabase.ShrineResultDbEnvelope
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}
//import slick.driver.H2Driver.api._

import scala.concurrent.duration._

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory H2 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
@RunWith(classOf[JUnitRunner])
class JsonStoreDatabaseTest extends FlatSpec with BeforeAndAfter with ScalaFutures with Matchers {
  implicit val timeout = 10.seconds
  val connector = JsonStoreDatabase.DatabaseConnector
  val IO = connector.IO
  val testShrineResults = Seq(
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableChangeCount = 0,queryId = UUID.randomUUID(),json = "todo"),
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableChangeCount = 0,queryId = UUID.randomUUID(),json = "todo"),
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableChangeCount = 0,queryId = UUID.randomUUID(),json = "todo"),
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableChangeCount = 0,queryId = UUID.randomUUID(),json = "todo"),
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableChangeCount = 0,queryId = UUID.randomUUID(),json = "todo")
  )

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
//    connector.executeTransactionBlocking(connector.IO.shrineResults ++= testShrineResults)
/*
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
*/
  }
}
