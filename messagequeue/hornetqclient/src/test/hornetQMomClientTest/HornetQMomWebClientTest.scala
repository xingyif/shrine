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
val firstQueue = HornetQMomWebClient.createQueueIfAbsent("q1")
HornetQMomWebClient.send("firstMessage", firstQueue)
import scala.concurrent.duration.Duration
val firstDuration = Duration.create(1, "seconds")
val receivedMsg = HornetQMomWebClient.receive(firstQueue, firstDuration)
val msg = receivedMsg.get
val allQueues = HornetQMomWebClient.queues
HornetQMomWebClient.completeMessage(msg)
HornetQMomWebClient.deleteQueue("q1")