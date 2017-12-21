package net.shrine.messagequeueservice

import scala.concurrent.Future

/**
  * A Message Trait that is implemented by SimpleMessage and MessageQueueClientMessage
  * Created by yifan on 9/8/17.
  */

trait Message {
  def complete(): Future[Unit]
  def contents: String
}

object Message {
  val contentsKey = "contents"
}