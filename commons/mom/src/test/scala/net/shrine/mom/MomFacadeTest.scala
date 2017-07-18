package net.shrine.mom

import java.util.Date

import org.hornetq.api.core.TransportConfiguration
import org.hornetq.api.core.client.HornetQClient
import org.hornetq.core.config.impl.ConfigurationImpl
import org.hornetq.core.remoting.impl.invm.{InVMAcceptorFactory, InVMConnectorFactory}
import org.hornetq.core.server.HornetQServers
import org.junit.runner.RunWith
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

/**
  * Test creation, insertion, querying, and deletion of ProblemDigest values into an
  * in-memory H2 database. Demonstrates proof of concept mapping of ProblemDigest
  * case class into a database.
  */
@RunWith(classOf[JUnitRunner])
class MomFacadeTest extends FlatSpec with BeforeAndAfter with ScalaFutures with Matchers {

  "HornetQ" should "be able to send and receive a messages" in {
    // Step 1. Create the Configuration, and set the properties accordingly
    val hornetQConfiguration = new ConfigurationImpl()
    hornetQConfiguration.setJournalDirectory("target/data/journal")
    hornetQConfiguration.setPersistenceEnabled(false)
    hornetQConfiguration.setSecurityEnabled(false)
    hornetQConfiguration.getAcceptorConfigurations.add(new TransportConfiguration(classOf[InVMAcceptorFactory].getName))

    // Step 2. Create and start the server
    val hornetQServer = HornetQServers.newHornetQServer(hornetQConfiguration)
    hornetQServer.start()

    // Step 3. As we are not using a JNDI environment we instantiate the objects directly
    val serverLocator = HornetQClient.createServerLocatorWithoutHA(new TransportConfiguration(classOf[InVMConnectorFactory].getName))
    val sf = serverLocator.createSessionFactory()

    // Step 4. Create a core queue
    val coreSession = sf.createSession(false, false, false)

    val queueName = "queue.exampleQueue"

    coreSession.createQueue(queueName, queueName, true)

    coreSession.close()

    // Step 5. Create the session, and producer
    val session = sf.createSession()

    val producer = session.createProducer(queueName)

    // Step 6. Create and send a message
    val message = session.createMessage(false)

    val propName = "myprop"

    message.putStringProperty(propName, "Hello sent at " + new Date())

    System.out.println("Sending the message.")

    producer.send(message)

    // Step 7. Create the message consumer and start the connection
    val messageConsumer = session.createConsumer(queueName)
    session.start()

    // Step 8. Receive the message.
    val  messageReceived = messageConsumer.receive(1000)
    System.out.println("Received TextMessage:" + messageReceived.getStringProperty(propName))

    // Step 9. Be sure to close our resources!
    if (sf != null)
    {
      sf.close()
    }

    // Step 10. Stop the server
    hornetQServer.stop()

  }

  /*
  implicit val timeout = 10.seconds
  val connector = JsonStoreDatabase.DatabaseConnector
  val IO = JsonStoreDatabase.IOActions

  val testShrineResults = Seq(
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableVersion = 0,queryId = UUID.randomUUID(),json = "todo"),
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableVersion = 1,queryId = UUID.randomUUID(),json = "todo"),
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableVersion = 2,queryId = UUID.randomUUID(),json = "todo"),
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableVersion = 3,queryId = UUID.randomUUID(),json = "todo"),
    ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 0,tableVersion = 4,queryId = UUID.randomUUID(),json = "todo")
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

  "The Database" should "handle bulk insert, transactional first-time upsert, and second-time upsert" in {
    // Insert the test records
    connector.runBlocking(IO.insertShrineResults(testShrineResults))

    // Test that they are all in the table
    var shrineResultContents = connector.runBlocking(IO.selectAll)
    shrineResultContents should contain theSameElementsAs testShrineResults
    shrineResultContents should have length testShrineResults.length

    // Reset the table
    connector.runBlocking(IO.clearTable >> IO.selectAll) shouldBe empty

    //Insert one at a time in a transaction
    val actions = testShrineResults.map(IO.upsertShrineResult)

    actions.map(connector.runBlocking(_))
    // Test that they are all in the table
    shrineResultContents = connector.runBlocking(IO.selectAll)
    shrineResultContents should contain theSameElementsAs testShrineResults
    shrineResultContents should have length testShrineResults.length

    val nextTestShrineResult = testShrineResults.head.copy(version = 1,tableVersion = 5,json="updated json")
    connector.runBlocking(IO.upsertShrineResult(nextTestShrineResult))

    val expectedSeq: Seq[ShrineResultDbEnvelope] = nextTestShrineResult +: testShrineResults.tail

    // Test that the new shrineResult is in the table
    shrineResultContents = connector.runBlocking(IO.selectAll)
    shrineResultContents should contain theSameElementsAs expectedSeq
    shrineResultContents should have length testShrineResults.length
  }

  "The Database" should "support queries of the last table change" in {
    // Insert the test records
    connector.runBlocking(IO.insertShrineResults(testShrineResults))

    // Test that they are all in the table
    val shrineResultContents = connector.runBlocking(IO.selectAll)
    shrineResultContents should contain theSameElementsAs testShrineResults
    shrineResultContents should have length testShrineResults.length

    val beforeChange: Seq[Option[Int]] = connector.runBlocking(IO.selectLastTableChange)
    val expectedBeforeChange = Seq(Some(4))
    beforeChange should equal(expectedBeforeChange)

    val nextTestShrineResult = testShrineResults.head.copy(version = 1,tableVersion = 5,json="updated json")
    connector.runBlocking(IO.upsertShrineResult(nextTestShrineResult))

    val lastChange: Seq[Option[Int]] = connector.runBlocking(IO.selectLastTableChange)
    val expectedTableChange = Seq(Some(5))
    lastChange should equal(expectedTableChange)
  }

  "The Database" should "support queries by parameters" in {
    // Insert the test records
    connector.runBlocking(IO.insertShrineResults(testShrineResults))

    val all = ShrineResultQueryParameters()

    // Test that they are all in the table
    val shrineResultContents = connector.runBlocking(IO.selectAll)
    shrineResultContents should contain theSameElementsAs testShrineResults
    shrineResultContents should have length testShrineResults.length

    val nextTestShrineResult = testShrineResults.head.copy(version = 1,tableVersion = 5,json="updated json")
    connector.runBlocking(IO.upsertShrineResult(nextTestShrineResult))

    val expectedSeq: Seq[ShrineResultDbEnvelope] = nextTestShrineResult +: testShrineResults.tail

    connector.runBlocking(IO.countWithParameters(all)) should equal(testShrineResults.length)
    connector.runBlocking(IO.selectResultsWithParameters(all)) should contain theSameElementsAs expectedSeq

    val afterTableChange = all.copy(afterTableChange = Some(4))
    connector.runBlocking(IO.countWithParameters(afterTableChange)) should equal(expectedSeq.count(_.tableVersion > 4))
    connector.runBlocking(IO.selectResultsWithParameters(afterTableChange)) should contain theSameElementsAs expectedSeq.filter(_.tableVersion > 4)

    val expectedWithQueryIds = Seq(expectedSeq.head,expectedSeq(2),expectedSeq(4))
    val queryIds = expectedWithQueryIds.map(_.queryId).to[Set]
    val withQueryIds = all.copy(forQueryIds = Some(queryIds))

    connector.runBlocking(IO.countWithParameters(withQueryIds)) should equal(expectedWithQueryIds.length)
    connector.runBlocking(IO.selectResultsWithParameters(withQueryIds)) should contain theSameElementsAs expectedWithQueryIds

    val withQueryIdsAfterTableChange = all.copy(afterTableChange = Some(4),forQueryIds = Some(queryIds))

    connector.runBlocking(IO.countWithParameters(withQueryIdsAfterTableChange)) should equal(expectedWithQueryIds.count(_.tableVersion > 4))
    connector.runBlocking(IO.selectResultsWithParameters(withQueryIdsAfterTableChange)) should contain theSameElementsAs expectedWithQueryIds.filter(_.tableVersion > 4)

    val skipAndLimit = all.copy(skip = Some(2),limit = Some(2))
    connector.runBlocking(IO.selectResultsWithParameters(skipAndLimit)) should contain theSameElementsAs expectedSeq.slice(2, 4)
  }

  "The Database" should "support optimistic updates" in {
    //test putting new data
    val firstResult = ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 1,tableVersion = 0,queryId = UUID.randomUUID(),json = "todo")
    val expectedFirst = Seq(firstResult.copy(tableVersion = 1))
    connector.runTransactionBlocking(IO.putShrineResult(firstResult))
    // Test that the table is right
    val firstShrineResultContents = connector.runBlocking(IO.selectAll)
    firstShrineResultContents should contain theSameElementsAs expectedFirst
    firstShrineResultContents should have length expectedFirst.length

    //test a second put of new data
    val secondResult = ShrineResultDbEnvelope(id = UUID.randomUUID(),version = 1,tableVersion = 0,queryId = UUID.randomUUID(),json = "todo")
    val expectedSecond = Seq(firstResult.copy(tableVersion = 1),secondResult.copy(tableVersion = 2))
    connector.runTransactionBlocking(IO.putShrineResult(secondResult))
    // Test that the table is right
    val secondShrineResultContents = connector.runBlocking(IO.selectAll)
    secondShrineResultContents should contain theSameElementsAs expectedSecond
    secondShrineResultContents should have length expectedSecond.length

    //test put of a new version of the first data
    val oldFirstResult = connector.runBlocking(IO.selectById(firstResult.id)).get
    val newFirstResult = oldFirstResult.copy(json = "different json")
    val expectedThird = Seq(newFirstResult.copy(version =2, tableVersion = 3),secondResult.copy(tableVersion = 2))
    connector.runTransactionBlocking(IO.putShrineResult(newFirstResult))
    // Test that the table is right
    val thirdShrineResultContents = connector.runBlocking(IO.selectAll)
    thirdShrineResultContents should contain theSameElementsAs expectedThird
    thirdShrineResultContents should have length expectedThird.length

    //test failure with a stale put
    //test put of a new version of the first data

    val staleFirstResult = oldFirstResult.copy(json = "stale object's json")
    an [CouldNotRunDbIoActionException] should be thrownBy connector.runTransactionBlocking(IO.putShrineResult(staleFirstResult))

  }
  */
}
