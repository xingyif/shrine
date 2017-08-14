package net.shrine.qep.querydb

import java.util.UUID

import akka.actor.ActorSystem
import com.typesafe.config.Config
import net.shrine.audit.NetworkQueryId
import net.shrine.source.ConfigSource
import net.shrine.config.ConfigExtensions

import scala.concurrent.Promise
import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}
import scala.concurrent.duration.{Duration, FiniteDuration}

import scala.concurrent.ExecutionContext.Implicits.global

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
case class QepQueryDbChangeNotifier(actorSystem: ActorSystem) {

  val config:Config = ConfigSource.config.getConfig("shrine.queryEntryPoint.changeNotifier")

  //todo when this grows up to support generic json data replace unit with the structure that was changed
  val unit:Unit = ()

  case class Trigger(requestId:UUID, networkQueryId: NetworkQueryId, promise: Promise[Unit], instertTime:Long)

  //todo when you handle general json data, expand NetworkQueryId into a p: A => Boolean for filter, to be evaulated as part of the scan, and pass in the changed object. Maybe even replace unit with the changed object
  val longPollRequestsToComplete:ConcurrentMap[UUID,Trigger] = TrieMap.empty

  /* scan all the pending Promises to see if any can be fulfilled */
  def triggerDataChangeFor(queryId:NetworkQueryId) = longPollRequestsToComplete.values.filter(_.networkQueryId == queryId).map(_.promise.trySuccess(unit))

  val interval: FiniteDuration = Some(config.get("interval",Duration(_))).collect { case d: FiniteDuration => d }.get

  actorSystem.scheduler.schedule(interval,interval)({
    val expireTime = System.currentTimeMillis() - interval.toMillis
    longPollRequestsToComplete.retain((id,x) => x.instertTime <= expireTime)
  })

  def putLongPollRequest(requstId:UUID,queryId:NetworkQueryId,promise: Promise[Unit]) = longPollRequestsToComplete.put(
    requstId,
    Trigger(requstId,queryId,promise,System.currentTimeMillis()))

  def removeLongPollRequest(requestId:UUID) = longPollRequestsToComplete.remove(requestId)
}
