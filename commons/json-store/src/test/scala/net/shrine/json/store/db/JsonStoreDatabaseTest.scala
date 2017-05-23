package net.shrine.json.store.db

import java.util.UUID

import net.shrine.json.store.db.JsonStoreDatabase.IOActions.NoOperation
import net.shrine.json.store.db.JsonStoreDatabase.ShrineResultDbEnvelope
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

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
  val IO = JsonStoreDatabase.IOActions
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
    // Insert the test records
    connector.executeTransactionBlocking(IO.insertShrineResults(testShrineResults))

    // Test that they are all in the table
    var shrineResultContents = connector.runBlocking(IO.selectAll)
    shrineResultContents should contain theSameElementsAs testShrineResults
    shrineResultContents should have length testShrineResults.length

    // Reset the table
    connector.runBlocking(IO.clearTable >> IO.selectAll) shouldBe empty

    //Insert one at a time in a transaction
    val actions = testShrineResults.map(IO.upsertShrineResult)

    connector.executeTransactionBlocking(actions:_*)
    // Test that they are all in the table
    shrineResultContents = connector.runBlocking(IO.selectAll)
    shrineResultContents should contain theSameElementsAs testShrineResults
    shrineResultContents should have length testShrineResults.length

    //todo test more low-level queries
  }
}
