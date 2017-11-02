package net.shrine.messagequeuemiddleware

import akka.actor.ActorRefFactory
import net.shrine.source.ConfigSource
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.StatusCodes.NotFound
import spray.testkit.ScalatestRouteTest

/**
  * Test to make sure MessageQueueWebApi is only available when correctly configured
  * Created by yifan on 9/8/17.
  */


@RunWith(classOf[JUnitRunner])
class MessageQueueWebApiConfigTest extends FlatSpec with ScalatestRouteTest with MessageQueueWebApi {
  override def actorRefFactory: ActorRefFactory = system

  private val queueName = "testQueueInConfig"

  "MessageQueueWebApi" should "block user from using the API and return a 404 response" in {
    ConfigSource.atomicConfig.configForBlock("shrine.messagequeue.blockingqWebApi.enabled", "false", "MessageQueueWebApiConfigTest") {
      Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
        val response = new String(body.data.toByteArray)

        assertResult(warningMessage)(response)
        assertResult(NotFound)(status)
      }
    }
  }
}