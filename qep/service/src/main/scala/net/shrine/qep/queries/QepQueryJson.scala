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
  */

final class QueryJson(val json: Json) extends QueryJsonTrait {
  override def equals(that: scala.Any): Boolean = that match {
    case that:QueryJson => QueryJson.unapply(this) == QueryJson.unapply(that)
    case _ => false
  }

  override def hashCode(): QueryId = default.hash(QueryJson.unapply(this).get)
}

object QueryJson {
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

trait QueryResultJsonTrait {
  val json: Json
  val queryResultId: UUID = json.queryResultId.as[UUID]
}

final class QueryResultJson(val json: Json) extends QueryResultJsonTrait {
  override def equals(that: scala.Any): Boolean = that match {
    case that:QueryResultJson => QueryResultJson.unapply(this) == QueryResultJson.unapply(that)
    case _ => false
  }

  override def hashCode(): QueryId = Objects.hash(QueryResultJson.unapply(this).get)
}

object QueryResultJson {
  def unapply(queryResultJson: QueryResultJson):Option[UUID] = {
    Some(queryResultJson.queryResultId)
  }
}

object Extractors {
  implicit val nodeExtractor: Extractor[Node, Json] = Json.extractor[String].map(XML.loadString)
  implicit val uuidExtractor: Extractor[UUID, Json] = Json.extractor[String].map(UUID.fromString)
  implicit val qresExtractor: Extractor[QueryResultJson, Json] = Json.extractor[Json].map(new QueryResultJson(_))
  implicit val querExtractor: Extractor[QueryJson, Json] = Json.extractor[Json].map(new QueryJson(_))
}
