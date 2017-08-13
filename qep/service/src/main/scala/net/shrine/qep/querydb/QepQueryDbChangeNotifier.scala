package net.shrine.qep.querydb

import java.util.UUID

import net.shrine.audit.NetworkQueryId

import scala.concurrent.Promise

import scala.collection.concurrent.{TrieMap, Map => ConcurrentMap}

/**
  *
  *
  * @author david 
  * @since 8/12/17
  */
//todo when we do the json data work, this class can grow up to be part of that subproject and leave here
object QepQueryDbChangeNotifier {

  val unit:Unit = ()
  //todo for when you handle general json data, expand NetworkQueryId into a p: A => Boolean for filter, to be evaulated as part of the scan, and pass in the changed object. Maybe even replace unit with the changed object
  val longPollRequestsToComplete:ConcurrentMap[UUID,(NetworkQueryId,Promise[Unit])] = TrieMap.empty

  /* scan all the pending Promises to see if any can be fulfilled */
  def triggerDataChangeFor(queryId:NetworkQueryId) = longPollRequestsToComplete.values.filter(_._1 == queryId).map(_._2.trySuccess(unit))

  def putLongPollRequest(requstId:UUID,queryId:NetworkQueryId,promise: Promise[Unit]) = longPollRequestsToComplete.put(requstId,(queryId,promise))

  def removeLongPollRequest(requestId:UUID) = longPollRequestsToComplete.remove(requestId)

}
