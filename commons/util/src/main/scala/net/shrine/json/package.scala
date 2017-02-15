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
  *        if an exception is thrown. Easy pattern is to call .get on an option.
  *        See <a>https://github.com/propensive/rapture/blob/dev/doc/json.md</a> for rapture docs,
  *        the website is slightly out of date
  */
package object json {
  implicit val node: Extractor[Node, Json] =
    Json.extractor[String].map(XML.loadString)
  implicit val nodeSeq: Extractor[NodeSeq, Json] =
    Json.extractor[String].map(XML.loadString)
  implicit val uuid: Extractor[UUID, Json] =
    Json.extractor[String].map(UUID.fromString)
  implicit val query: Extractor[Query, Json] =
    Json.extractor[Json].map(new Query(_))
  implicit val successResult: Extractor[SuccessResult, Json] =
    Json.extractor[Json].map(new SuccessResult(_))
  implicit val failureResult: Extractor[FailureResult, Json] =
    Json.extractor[Json].map(new FailureResult(_))
  implicit val pendingResult: Extractor[PendingResult, Json] =
    Json.extractor[Json].map(new PendingResult(_))
  implicit val queryResult: Extractor[QueryResult, Json] =
    successResult
      .orElse(failureResult)
      .orElse(pendingResult)
}
