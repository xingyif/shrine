package net.shrine.json

import java.util.UUID

import rapture.json._

import scala.util.Try
import scala.util.hashing.Hashing.default
import scala.xml.Node

/**
  * @author ty
  * @since 1/31/17
  */
final class Query(val json: Json) {
  val queryId: UUID = json.queryId.as[UUID]
  val topicId: UUID = json.topicId.as[UUID]
  val userId: UUID = json.userId.as[UUID]
  val startTime: Long = json.startTime.as[Long]
  val i2b2QueryText: Node = json.i2b2QueryText.as[Node]
  val extraXml: Node = json.extraXml.as[Node]
  val queryResults: List[UUID] = json.queryResults.as[List[UUID]]

  private[Query] val fields = (queryId,
                               topicId,
                               userId,
                               startTime,
                               i2b2QueryText,
                               extraXml,
                               queryResults)
  // Structural equality on everything except the underlying json.
  // Can always get full equality by this.json == that.json
  override def equals(that: scala.Any): Boolean = that match {
    case that: Query => this.fields == that.fields
    case _ => false
  }

  override def hashCode(): Int = default.hash(fields)

  override def toString: String = json.toBareString
}

object Query {
  def apply(json: Json): Option[Query] =
    Try(new Query(json)).toOption

  // Allows easier pattern matching on all the fields we care about.
  def unapply(arg: Query) = Some(arg.fields)
}
