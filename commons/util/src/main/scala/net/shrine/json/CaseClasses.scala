package net.shrine.json

import java.util.UUID

import net.shrine.problem.ProblemDigest
import rapture.json._
import jsonBackends.jawn._

import scala.xml.Node

// The Adapter, Topic, and User entities don't have to
// carry arbitrary json, so they can just be case classes
case class Adapter(name: String, id: UUID)

case class Topic(name: String, description: String, id: UUID)

case class User(userName: String, domain: String, id: UUID)

case class Breakdown(category: String, results: List[BreakdownProperty])

case class BreakdownProperty(name: String, count: Int)

case class NoiseTerms(clamp: Int, sigma: Int, rounding: Int)

case class NestedQuery(queryId: UUID,
                       topic: Topic,
                       user: User,
                       startTime: Long,
                       i2b2QueryText: Node,
                       extraXml: Node,
                       queryResults: List[NestedQueryResult])

sealed trait NestedQueryResult {
  val adapter : Adapter
  val status  : String
  val resultId: UUID
  val queryId : UUID
}

case class NSuccessResult(resultId: UUID,
                          queryId: UUID,
                          adapter: Adapter,
                          count: Int,
                          noiseTerms: NoiseTerms,
                          i2b2Mapping: Node,
                          flags: List[String],
                          breakdowns: List[Breakdown])
    extends NestedQueryResult {
  override val status: String = Statuses.success
}

object NSuccessResult {

  import Statuses._

  def apply(json: Json): Option[NSuccessResult] =
    if (eq(json.status, success) && json.is[NSuccessResult])
      Some(json.as[NSuccessResult])
    else None
}

case class NPendingResult(resultId: UUID, queryId: UUID, adapter: Adapter)
    extends NestedQueryResult {
  override val status: String = Statuses.pending
}

object NPendingResult {

  import Statuses._

  def apply(json: Json): Option[NPendingResult] =
    if (eq(json.status, pending) && json.is[NPendingResult])
      Some(json.as[NPendingResult])
    else None
}

case class NFailureResult(resultId: UUID,
                          queryId: UUID,
                          adapter: Adapter,
                          problemDigest: ProblemDigest)
    extends NestedQueryResult {
  override val status: String = Statuses.failure
}

object NFailureResult {

  import Statuses._

  def apply(json: Json): Option[NFailureResult] =
    if (eq(json.status, failure) && json.is[NFailureResult])
      Some(json.as[NFailureResult])
    else None
}

object Statuses {
  val success = "success"
  val pending = "pending"
  val failure = "failure"
  def eq(json: Json, string: String) =
    json.is[String] && json.as[String].toLowerCase == string
}

case class JsonQuery(json: Json, nestedQuery: NestedQuery) {
  val normalized:Seq[(Query, QueryResult, User, Topic, Adapter)] = {
    val qb = toBuffer(json)
    val qrs = json.queryResults.as[List[Json]].map(rj => {
      val rb = toBuffer(rj)
      val a = rb.adapter.as[Adapter]
      rb.adapterId = a.id
      rb -= "adapter"
      (rb.as[QueryResult], a)
    })
    val user = nestedQuery.user
    val topic = nestedQuery.topic
    qb.userId = user.id
    qb.topicId = topic.id
    qb.queryResults = nestedQuery.queryResults.map(_.resultId)
    qb -= "user"
    qb -= "topic"
    qb -= "queryResults"
    val query = qb.as[Query]

    qrs.map{
      case (result, adapter) => (query, result, user, topic, adapter)
    }
  }
}

object JsonQuery {
  def apply(json: Json): Option[JsonQuery] =
    if (json.is[NestedQuery])
      Some(JsonQuery(json, json.as[NestedQuery]))
    else None
}
