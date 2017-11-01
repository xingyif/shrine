package net.shrine.messagequeueservice

import scala.util.Try

/**
  * A Message Trait that is implemented by SimpleMessage and MessageQueueClientMessage
  * Created by yifan on 9/8/17.
  */

trait Message {
  def complete(): Try[Unit]
  def contents: String
}

object Message {
  val contentsKey = "contents"
}