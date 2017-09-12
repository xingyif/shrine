/**
  * A simple scala script that test HornetQMomWedClient
  *
  * Load the file in scala REPL to test HornetQMomWebClient
  *
  * Created by yifan on 8/14/17.
  */
// To Run the test, simply start the scala REPL and load the file
// Enter the following commands using the command line

// #1. Start scala REPL with 2.11.8 version
// mvn -Dscala-version="2.11.8" scala:console

// #2. Load file in REPL:
// :load <path-to-file>

import net.shrine.hornetqclient.HornetQMomWebClient
import net.shrine.messagequeueservice.{Message, Queue}
import scala.collection.immutable.Seq
val firstQueue: Queue = HornetQMomWebClient.createQueueIfAbsent("firstQueue").get
HornetQMomWebClient.send("firstMessage", firstQueue)
import scala.concurrent.duration.Duration
val firstDuration: Duration = Duration.create(1, "seconds")
val receivedMsg: Option[Message] = HornetQMomWebClient.receive(firstQueue, firstDuration).get
val msg: Message = receivedMsg.get
val allQueues: Seq[Queue] = HornetQMomWebClient.queues.get
msg.complete()
HornetQMomWebClient.deleteQueue("q1")