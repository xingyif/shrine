package net.shrine.json
import java.util.UUID

import rapture.json._
import jsonBackends.jawn._

import scala.xml.{Node, NodeSeq, XML}

/**
  * @author ty
  * @since 2/1/17
  * Defining an extractor gives you the .is and .as methods on Json objects.
  * .is reports true if the extract completes the extraction, and returns false
  * if an exception is thrown. Easy pattern is to call .get on an option
  */
// Todo: Move into package object. Where?
object Extractors {
  implicit val nodeExtractor: Extractor[Node, Json] = Json.extractor[String].map(XML.loadString)
  implicit val nseqExtractor: Extractor[NodeSeq, Json] = Json.extractor[String].map(XML.loadString)
  implicit val uuidExtractor: Extractor[UUID, Json] = Json.extractor[String].map(UUID.fromString)
  implicit val querExtractor: Extractor[Query, Json]       = Json.extractor[Json].map(Query(_).get)
  implicit val qresExtractor: Extractor[QueryResult, Json] = Json.extractor[Json].map(QueryResult(_).get)
  implicit val brekExtractor: Extractor[Breakdown, Json]   = Json.extractor[Json].map(
    json => Breakdown.fromStringResults(json.category.as[String], json.results.as[List[BreakdownProperty]]).get)
}


