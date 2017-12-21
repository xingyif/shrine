/**
  * A simple scala script that test MessageQueueWedClient
  *
  * Load the file in scala REPL to test MessageQueueWebClient
  *
  * Created by yifan on 8/14/17.
  */
// To Run the test, simply start the scala REPL and load the file
// Enter the following commands using the command line

// #1. Start scala REPL with 2.11.8 version
// mvn -Dscala-version="2.11.8" scala:console

// #2. Load file in REPL:
// :load <path-to-file>

import net.shrine.messagequeueclient.MessageQueueWebClient
import net.shrine.messagequeueservice.{Message, Queue}
import scala.collection.immutable.Seq
import scala.concurrent.Await

val firstQueue: Queue = MessageQueueWebClient.createQueueIfAbsent("firstQueue").get
MessageQueueWebClient.send("firstMessage", firstQueue)
import scala.concurrent.duration.Duration
val firstDuration: Duration = Duration.create(1, "seconds")
val receivedMsg: Option[Message] = Await.result(MessageQueueWebClient.receive(firstQueue, firstDuration), firstDuration)
assert(receivedMsg.isDefined)
val msg: Message = receivedMsg.get
val allQueues: Seq[Queue] = MessageQueueWebClient.queues.get
val receivedMsg2: Option[Message] = Await.result(MessageQueueWebClient.receive(firstQueue, firstDuration), firstDuration)
assert(receivedMsg2.isEmpty)
msg.complete()
val receivedMsg3: Option[Message] = Await.result(MessageQueueWebClient.receive(firstQueue, firstDuration), firstDuration)
assert(receivedMsg3.isEmpty)
MessageQueueWebClient.deleteQueue("firstQueue")