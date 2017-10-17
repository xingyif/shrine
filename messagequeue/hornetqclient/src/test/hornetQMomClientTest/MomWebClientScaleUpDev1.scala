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

import java.util

import net.shrine.hornetqclient.HornetQMomWebClient
import net.shrine.messagequeueservice.{Message, Queue}
import net.shrine.source.ConfigSource

import scala.concurrent.duration.Duration
import scala.collection.immutable.Seq
import scala.collection.mutable.ArrayBuffer

  val configMap: Map[String, String] = Map( "shrine.messagequeue.blockingq.serverUrl" -> "https://shrine-dev1.catalyst:6443/shrine-metadata/mom")

  ConfigSource.atomicConfig.configForBlock(configMap, "HornetQMomClientDev1") {
    val scale: Int = 10
    val multiplier: Int = 3
    println(s"Running tests on ${HornetQMomWebClient.momUrl}")

    val queueNameDev1: String = "dev1Queue"
    val dev2Queue: Queue = HornetQMomWebClient.createQueueIfAbsent(queueNameDev1).get
    val allQueues: Seq[Queue] = HornetQMomWebClient.queues.get
    println(s"All Existing Queues: $allQueues")

    for (i <- 1 to scale) {
      val sendTry = HornetQMomWebClient.send(s"Message$i sent to $queueNameDev1", dev2Queue)
      println(s"Sending messages to dev1, attempt: $i, $sendTry")
    }

    val firstDuration: Duration = Duration.create(1, "seconds")
    val listOfMessages: ArrayBuffer[Option[Message]] = ArrayBuffer()
    for (i <- 1 to scale * multiplier) {
      val receivedOpt: Option[Message] = HornetQMomWebClient.receive(dev2Queue, firstDuration).get
      listOfMessages += receivedOpt
      println(s"Receiving messages to dev1, attempt: $i, $receivedOpt")
    }

    listOfMessages.foreach(msgOpt => msgOpt.map(m => m.complete()))
//    for (i <-1 to 10) {
//      val messageOpt: Option[Message] = listOfMessages(i)
//      messageOpt.map(m => m.complete())
//    }

    val deleteTry = HornetQMomWebClient.deleteQueue(queueNameDev1)
    println(s"Deleted queue: $queueNameDev1, $deleteTry")
  }