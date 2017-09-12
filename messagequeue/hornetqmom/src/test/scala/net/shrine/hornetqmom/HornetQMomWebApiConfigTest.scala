package net.shrine.hornetqmom

import akka.actor.ActorRefFactory
import net.shrine.source.ConfigSource
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import spray.http.StatusCodes.NotFound
import spray.testkit.ScalatestRouteTest

/**
  * Test to make sure hornetQMomWebApi is only available when correctly configured
  * Created by yifan on 9/8/17.
  */


@RunWith(classOf[JUnitRunner])
class HornetQMomWebApiConfigTest extends FlatSpec with ScalatestRouteTest with HornetQMomWebApi {
  override def actorRefFactory: ActorRefFactory = system

  private val queueName = "testQueue"


  override val enabled: Boolean = ConfigSource.config.getBoolean("shrine.messagequeue.hornetQWebApiTest.enabled")

  "HornetQMomWebApi" should "block user from using the API and return a 404 response" in {

    Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
      val response = new String(body.data.toByteArray)

      assertResult(warningMessage)(response)
      assertResult(NotFound)(status)
    }
  }
}