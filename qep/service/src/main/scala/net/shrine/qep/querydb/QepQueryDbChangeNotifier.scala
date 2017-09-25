package net.shrine.qep.querydb

import java.util.UUID
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.typesafe.config.Config
import net.shrine.audit.NetworkQueryId
import net.shrine.config.ConfigExtensions
import net.shrine.log.Log
import net.shrine.source.ConfigSource

import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}
import scala.concurrent.Promise
import scala.concurrent.duration.Duration

/**
  * Notifies pending long poll http requests of changes in the data.
  *
  * This is an early start piece to support a much more general service when we have general json structures in SHRINE
  *
  * @author david 
  * @since 8/12/17
  */
//todo when we do the json data work, this class can grow up to be part of that subproject and leave here
//todo maybe use a separate actor system, if needed, to let the QepQueryDb or something wrapping it, receive messages in the QEP data. The actorSystem is only used for scheduling. SHRINE-2167
object QepQueryDbChangeNotifier {

  val config:Config = ConfigSource.config.getConfig("shrine.queryEntryPoint.changeNotifier")

  //todo when this grows up to support generic json data replace unit with the structure that was changed
  val unit:Unit = ()

  case class Trigger(requestId:UUID, networkQueryId: NetworkQueryId, promise: Promise[Unit], instertTime:Long)

  //todo when you handle general json data, expand NetworkQueryId into a p: A => Boolean for filter, to be evaluated as part of the scan, and pass in the changed object. Maybe even replace unit with the changed object
  val longPollRequestsToComplete:ConcurrentMap[UUID,Trigger] = TrieMap.empty

  def putLongPollRequest(requstId:UUID,queryId:NetworkQueryId,promise: Promise[Unit]) = {
    longPollRequestsToComplete.put(
      requstId,
      Trigger(requstId,queryId,promise,System.currentTimeMillis()))

    Log.debug(s"putLongPollRequest Pending requests when triggering $queryId: $longPollRequestsToComplete")
  }

  def removeLongPollRequest(requestId:UUID) = longPollRequestsToComplete.remove(requestId)

  /* scan all the pending Promises to see if any can be fulfilled */
  def triggerDataChangeFor(queryId:NetworkQueryId):Unit = {
    val fulfilled: Iterable[NetworkQueryId] = longPollRequestsToComplete.values.filter(_.networkQueryId == queryId).map{ trigger:Trigger =>
      if(trigger.promise.trySuccess(unit)) Log.debug(s"Successfully triggered $trigger for $queryId")
      queryId
    }
    fulfilled.headOption.getOrElse(Log.debug(s"No promises fulfilled for $queryId"))
  }

//respond after time out
  val interval: Duration = config.get("interval",Duration(_))
  val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1) //todo better way to stop the scheduler

  val runnable:Runnable = new Runnable {
    override def run(): Unit = {
      val expireTime = System.currentTimeMillis() - interval.toMillis
      longPollRequestsToComplete.retain((id, x) => x.instertTime <= expireTime)
    }
  }

  scheduler.scheduleAtFixedRate(runnable,interval.toMillis,interval.toMillis,TimeUnit.MILLISECONDS)
}
