package net.shrine.qep.queries

import java.util.{Objects, UUID}

import rapture.json._
import rapture.json.jsonBackends.jawn._
import Extractors._

import scala.xml.{Node, XML}
import scala.util.hashing.Hashing.default

/**
  * @by ty
  * @since 1/31/17
  * This uses a class, object, and trait to define one entity.
  * Should I just get rid of the trait altogether?
  * Also, everything in the class and companion object will be the same,
  * except for the fields that it's working with. Would be a good place
  * to introduce a macro to reduce boilerplate, possibly
  */

final class QueryJson(val json: Json) extends QueryJsonTrait {
  // Structural equality on everything except the underlying json.
  // Can always get full equality by this.json == that.json
  override def equals(that: scala.Any): Boolean = that match {
    case that:QueryJson => QueryJson.unapply(this) == QueryJson.unapply(that)
    case _ => false
  }

  override def hashCode(): QueryId = default.hash(QueryJson.unapply(this).get)
}

object QueryJson {
  def apply(json:Json): Option[QueryJson] =
    if (json.is[QueryJson]) Some(json.as[QueryJson]) else None

  // Allows easier pattern matching on all the fields we care about.
  def unapply(arg: QueryJson): Option[(UUID, UUID, UUID, Long, Node, Node, List[QueryResultJson])] =
    Some((arg.queryId, arg.topicId, arg.userId, arg.startTime, arg.i2b2QueryText, arg.extraXml, arg.queryResults))
}

trait QueryJsonTrait {
  val json: Json
  val queryId: UUID = json.queryId.as[UUID]
  val topicId: UUID = json.topicId.as[UUID]
  val userId : UUID = json.userId.as[UUID]
  val startTime: Long     = json.startTime.as[Long]
  val i2b2QueryText: Node = json.i2b2QueryText.as[Node]
  val extraXml     : Node = json.extraXml.as[Node]
  val queryResults : List[QueryResultJson] = json.queryResults.as[List[QueryResultJson]]
}

final class QueryResultJson(val json: Json) extends QueryResultJsonTrait {
  override def equals(that: scala.Any): Boolean = that match {
    case that:QueryResultJson => QueryResultJson.unapply(this) == QueryResultJson.unapply(that)
    case _ => false
  }

  override def hashCode(): QueryId = Objects.hash(QueryResultJson.unapply(this).get)
}

object QueryResultJson {
  def apply(json:Json): Option[QueryResultJson] =
    if (json.is[QueryResultJson]) Some(json.as[QueryResultJson]) else None

  def unapply(queryResultJson: QueryResultJson):Option[UUID] = {
    Some(queryResultJson.queryResultId)
  }
}

// Not done
trait QueryResultJsonTrait {
  val json: Json
  val queryResultId: UUID = json.queryResultId.as[UUID]
}

// Todo: Move into package object. Where?
object Extractors {
  implicit val nodeExtractor: Extractor[Node, Json] = Json.extractor[String].map(XML.loadString)
  implicit val uuidExtractor: Extractor[UUID, Json] = Json.extractor[String].map(UUID.fromString)
  implicit val qresExtractor: Extractor[QueryResultJson, Json] = Json.extractor[Json].map(new QueryResultJson(_))
  implicit val querExtractor: Extractor[QueryJson, Json] = Json.extractor[Json].map(new QueryJson(_))
}
