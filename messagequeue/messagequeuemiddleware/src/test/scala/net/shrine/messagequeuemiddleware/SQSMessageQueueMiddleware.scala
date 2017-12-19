package net.shrine.messagequeuemiddleware

import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.junit.JUnitRunner

/**
  * Test create, delete queue, send, and receive message, getQueueNames, and completeMessage using SQS service
  * Created by yifan on 12/19/17.
  */

@RunWith(classOf[JUnitRunner])
class SQSMessageQueueMiddleware extends FlatSpec with BeforeAndAfterAll with ScalaFutures with Matchers {

}