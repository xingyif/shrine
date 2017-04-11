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
  def toBuffer(json: Json): JsonBuffer =
    JsonBuffer.construct(json.$root.copy(), Vector())
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
  implicit val nSuccessResult: Extractor[NSuccessResult, Json] =
    Json.extractor[Json].map(NSuccessResult(_).get)
  implicit val nFailureResult: Extractor[NFailureResult, Json] =
    Json.extractor[Json].map(NFailureResult(_).get)
  implicit val nPendingResult: Extractor[NPendingResult, Json] =
    Json.extractor[Json].map(NPendingResult(_).get)
  implicit val nestedQueryResult: Extractor[NestedQueryResult, Json] =
    nSuccessResult
      .orElse(nFailureResult)
      .orElse(nPendingResult)
  implicit val nestedQuery: Extractor[NestedQuery, Json] =
    Json
      .extractor[Json]
      .map(
        js =>
          NestedQuery(
            js.queryID.as[UUID],
            js.topic.as[Topic],
            js.user.as[User],
            js.startTime.as[Long],
            js.i2b2QueryText.as[Node],
            js.extraXml.as[Node],
            js.queryResults.as[List[NestedQueryResult]]
        ))
  implicit val uuidSerializer: AnyRef with Serializer[UUID, Json] =
    Json.serializer[Json].contramap[UUID](uuid => Json(uuid.toString))
}
