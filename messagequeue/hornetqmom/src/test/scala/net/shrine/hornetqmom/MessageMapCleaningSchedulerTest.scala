//package net.shrine.hornetqmom
//
//import java.util.concurrent.TimeUnit
//
//import akka.actor.ActorRefFactory
//import net.shrine.messagequeueservice.Queue
//import net.shrine.source.ConfigSource
//import org.json4s.NoTypeHints
//import org.json4s.native.Serialization
//import org.json4s.native.Serialization.read
//import org.junit.runner.RunWith
//import org.scalatest.FlatSpec
//import org.scalatest.junit.JUnitRunner
//import spray.http.HttpEntity
//import spray.http.StatusCodes.{Accepted, Created, NotFound}
//import spray.testkit.ScalatestRouteTest
//
///**
//  * Created by yifan on 9/15/17.
//  */
//
//@RunWith(classOf[JUnitRunner])
//class MessageMapCleaningSchedulerTest extends FlatSpec with ScalatestRouteTest with HornetQMomWebApi {
//  override def actorRefFactory: ActorRefFactory = system
//
//  private val proposedQueueName = "testmap Queue"
//  private val queue: Queue = Queue(proposedQueueName)
//  private val queueName: String = queue.name // "testmapQueue"
//  private val messageContent = "test Content in map"
//
//  "MessageMapCleaningScheduler" should "discard the message after it exceeds its timeout time" in {
//
//    ConfigSource.atomicConfig.configForBlock("shrine.messagequeue.hornetQWebApi.enabled", "true", "MessageMapCleaningSchedulerTest") {
//      ConfigSource.atomicConfig.configForBlock("shrine.messagequeue.hornetQWebApi.messageTimeToLive", "1 second", "MessageMapCleaningSchedulerTest") {
//
//        Put(s"/mom/createQueue/$queueName") ~> momRoute ~> check {
//          val response = new String(body.data.toByteArray)
//          println(response)
//          implicit val formats = Serialization.formats(NoTypeHints)
//          val jsonToQueue = read[Queue](response)(formats, manifest[Queue])
//          val responseQueueName = jsonToQueue.name
//          assertResult(Created)(status)
//          assertResult(queueName)(responseQueueName)
//        }
//
//        Put(s"/mom/sendMessage/$queueName", HttpEntity(s"$messageContent")) ~> momRoute ~> check {
//          assertResult(Accepted)(status)
//        }
//
//        TimeUnit.SECONDS.sleep(1)
//
//        Get(s"/mom/receiveMessage/$queueName?timeOutSeconds=2") ~> momRoute ~> check {
//          assertResult(NotFound)(status)
//        }
//      }
//    }
//  }
//}