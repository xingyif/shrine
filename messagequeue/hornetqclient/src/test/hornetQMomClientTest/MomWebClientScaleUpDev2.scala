/**
  * A simple scala script that scale up the test for running queries using shrine-dev2
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

import java.util.concurrent.{Executors, TimeUnit}

import net.shrine.hornetqclient.HornetQMomWebClient
import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.source.ConfigSource

import scala.collection.immutable.Seq
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.Duration

val configMap: Map[String, String] = Map( "shrine.messagequeue.blockingq.serverUrl" -> "https://shrine-dev2.catalyst:6443/shrine-metadata/mom")

ConfigSource.atomicConfig.configForBlock(configMap, "HornetQMomClientDev2") {
  val scale: Int = 60
  val multiplier: Int = 2
  println(s"Running tests on ${HornetQMomWebClient.momUrl}")


//  val queueNameDev1: String = "dev1Queue"
//  val dev1Queue: Queue = HornetQMomWebClient.createQueueIfAbsent(queueNameDev1).get
  val queueNameDev2: String = "dev2Queue"
  val dev2Queue: Queue = HornetQMomWebClient.createQueueIfAbsent(queueNameDev2).get

  val allQueues: Seq[Queue] = HornetQMomWebClient.queues.get
  println(s"All Existing Queues: $allQueues")

  for (i <- 1 to scale) {
    val sendTry = HornetQMomWebClient.send(s"Message$i sent to $queueNameDev2", dev2Queue)
    println(s"Sending messages to dev2, attempt: $i, $sendTry")
  }

  val firstDuration: Duration = Duration.create(15, "seconds")
  val listOfMessages: ArrayBuffer[Option[Message]] = ArrayBuffer()
  val executor = Executors.newFixedThreadPool(scale*multiplier)

  val worker: Runnable = new Runnable {
    override def run(): Unit = {
          val receivedOpt: Option[Message] = HornetQMomWebClient.receive(dev2Queue, firstDuration).get
          listOfMessages += receivedOpt
          println(s"Receiving messages from dev1, $receivedOpt, thread: ${Thread.currentThread().getId}")
        }
  }

  for (i <- 1 to scale * multiplier) {
//    println(s"Receiving messages from dev1, attempt: $i")
    executor.execute(worker)
  }

  listOfMessages.foreach(msgOpt => msgOpt.map({
    m => {
      m.complete()
      println(s"Completed Message $m")
    }
  }))

//  TimeUnit.SECONDS.wait(5)
  for (i <- 1 to scale) {
    val receivedOpt: Option[Message] = HornetQMomWebClient.receive(dev2Queue, firstDuration).get
    listOfMessages += receivedOpt
    println(s"Receiving messages from dev2, attempt: $i, $receivedOpt")
  }

//  val deleteTry = HornetQMomWebClient.deleteQueue(queueNameDev2)
//  println(s"Deleted queue: $queueNameDev2, $deleteTry")
}