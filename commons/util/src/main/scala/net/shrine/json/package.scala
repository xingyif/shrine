package net.shrine

import java.util.UUID

import rapture.json._
import jsonBackends.jawn._

import scala.xml.{Node, NodeSeq, XML}

/**
  * @author ty
  * @since 2/1/17
  *        Defining an extractor gives you the .is and .as methods on Json objects.
  *        .is reports true if the extract completes the extraction, and returns false
  *        if an exception is thrown. Easy pattern is to call .get on an option
  */
package object json {
  implicit val node: Extractor[Node, Json] =
    Json.extractor[String].map(XML.loadString)
  implicit val nodeSeq: Extractor[NodeSeq, Json] =
    Json.extractor[String].map(XML.loadString)
  implicit val uuid: Extractor[UUID, Json] =
    Json.extractor[String].map(UUID.fromString)
  implicit val query: Extractor[Query, Json] =
    Json.extractor[Json].map(Query(_).get)
  implicit val queryResult: Extractor[QueryResult, Json] =
    Json.extractor[Json].map(QueryResult(_).get)
  implicit val breakdown: Extractor[Breakdown, Json] =
    Json.extractor[Json]
      .map(json =>
          Breakdown
            .fromStringResults(json.category.as[String],
                               json.results.as[List[BreakdownProperty]])
            .get)
}
