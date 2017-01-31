package net.shrine.qep.queries

import java.util.{Objects, UUID}

import rapture.json._
import rapture.json.jsonBackends.jawn._
import Extractors._

import scala.xml.{Node, XML}

/**
  * @by ty
  * @since 1/31/17
  */

final class QueryJson(val json: Json) extends QueryJsonTrait {
  override def equals(that: scala.Any): Boolean = that match {
    case that:QueryJson =>
      this.queryId == that.queryId &&
      this.topicId == that.topicId &&
      this.userId == that.userId &&
      this.startTime == that.startTime &&
      this.i2b2QueryText == that.i2b2QueryText &&
      this.extraXml == that.extraXml &&
      this.queryResults == that.queryResults
    case _ => false
  }

  override def hashCode(): QueryId =
    Objects.hash(this.queryId, this.topicId, this.userId, this.startTime, this.i2b2QueryText, this.extraXml, this.queryResults)

}

trait QueryJsonTrait {
  val json: Json
  val queryId: UUID = json.queryId.as[UUID]
  val topicId: UUID = json.topicId.as[UUID]
  val userId : UUID = json.userId.as[UUID]
  val startTime: Long     = json.startTime.as[Long]
  val i2b2QueryText: Node = json.i2b2Querytext.as[Node]
  val extraXml     : Node = json.extraXml.as[Node]
  val queryResults : List[QueryResultJson] = json.queryResults.as[List[QueryResultJson]]
}

trait QueryResultJsonTrait {
  val json: Json
  val queryResultId: UUID = json.queryResultId.as[UUID]
}

final class QueryResultJson(val json: Json) extends QueryResultJsonTrait {
  override def equals(that: scala.Any): Boolean = that match {
    case that:QueryResultJson =>
      this.queryResultId == that.queryResultId
    case _ => false
  }

  override def hashCode(): QueryId = Objects.hash(this.queryResultId)
}

object Extractors {
  implicit val nodeExtractor: Extractor[Node, Json] = Json.extractor[String].map(XML.loadString)
  implicit val uuidExtractor: Extractor[UUID, Json] = Json.extractor[String].map(UUID.fromString)
  implicit val qresExtractor: Extractor[QueryResultJsonTrait, Json] = Json.extractor.map(new QueryResultJson(_))
  implicit val querExtractor: Extractor[QueryJsonTrait, Json] = Json.extractor.map(new QueryJson(_))
}
