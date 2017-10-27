/**
  * A simple scala script that scale up the test for running queries using shrine-dev1
  *
  * Load the file in scala REPL to run the tests
  *
  * Created by yifan on 10/17/17.
  */
// To Run the test, simply start the scala REPL and load the file
// Enter the following commands using the command line

// #1. Start scala REPL with 2.11.8 version
// mvn -Dscala-version="2.11.8" scala:console

// #2. Load file in REPL:
// :load <path-to-file>
import java.util.concurrent.Executors

import net.shrine.hornetqclient.HornetQMomWebClient
import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.source.ConfigSource

import scala.collection.immutable.Seq
import scala.concurrent.duration.Duration
import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}

val printStream: PrintStream = new PrintStream(new BufferedOutputStream(new FileOutputStream("ScaleUpTestOnDev1.txt")))
System.setOut(printStream)
System.setErr(printStream)
val configMap: Map[String, String] = Map( "shrine.messagequeue.blockingq.serverUrl" -> "https://shrine-dev1.catalyst:6443/shrine-metadata/mom")

ConfigSource.atomicConfig.configForBlock(configMap, "HornetQMomClientDev1") {

  val numberOfQEPs: Int = 62
  val numberOfMessages: Int = 5
  System.out.println(s"Running tests on ${HornetQMomWebClient.momUrl}")

  // create all queues and send messages each queue
  for (i <- 1 to numberOfQEPs) {
    val queueName: String = s"QEPQueue$i"
    val deleteTry = HornetQMomWebClient.deleteQueue(queueName)
    System.out.println(s"Deleted queue: $queueName, $deleteTry")

    val queue: Queue = HornetQMomWebClient.createQueueIfAbsent(queueName).get
    System.out.println(s"Created queue $queueName on QEP $i")
    for (i <- 1 to numberOfMessages) {
      val sendTry = HornetQMomWebClient.send(s"Message$i sent to $queueName", queue)
      System.out.println(s"Sending messages to dev1, attempt: $i, $sendTry")
    }
  }

  val allQueues: Seq[Queue] = HornetQMomWebClient.queues.get.filter(queue => {
    (queue.name != "shrinedev1") && (queue.name != "shrinedev2")
  })
  System.out.println(s"All Existing Queues: $allQueues")

  val firstDuration: Duration = Duration.create(15, "seconds")
  val executor = Executors.newFixedThreadPool(numberOfQEPs)

  allQueues.map(queue => {
    new Runnable {
      override def run(): Unit = {
        while (true) {
          val receivedOpt: Option[Message] = HornetQMomWebClient.receive(queue, firstDuration).get
          Thread.currentThread().setName(s"QEPQueue${queue.name}")
          System.out.println(s"Receiving messages from the HUB, $receivedOpt, thread: ${Thread.currentThread().getName}, id: ${Thread.currentThread().getId}")
          receivedOpt.map(msg => {
            val completeTry = msg.complete()
            System.out.println(s"Completed Message $msg, status: $completeTry")
          })
        }
      }
    }
  }).par.foreach(worker => executor.execute(worker))
}