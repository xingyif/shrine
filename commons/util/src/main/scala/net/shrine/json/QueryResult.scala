package net.shrine.json

import java.util.UUID

import net.shrine.problem.ProblemDigest
import rapture.json._

import scala.util.Try
import scala.util.hashing.Hashing.default
import scala.xml.Node

/**
  * @author ty
  * @since 2/1/17
  * A Query Result can either be a Success, Failure, or Pending result
  * Upon creation, it looks at the status field of the json to determine
  * which to create. Defines structural equality and unapply methods for each
  * while still being backed by the dynamic json ast
  */
object QueryResult {
  def apply(json: Json): Option[QueryResult] =
    SuccessResult(json)
      .orElse(PendingResult(json))
      .orElse(FailureResult(json))
}

sealed trait QueryResult {
  val json: Json
  val status: String = json.status.as[String]
}

            // <--=== Success Result ===--> //

final class SuccessResult(val json: Json) extends QueryResult {
  val resultId: UUID = json.resultId.as[UUID]
  val adapterId: UUID = json.adapterId.as[UUID]
  val count: Int = json.count.as[Int]
  val noiseTerms: NoiseTerms = json.noiseTerms.as[NoiseTerms]
  val i2b2Mapping: Node = json.i2b2Mapping.as[Node]
  // TODO: Once we figure out what flags are, replace them with a concrete type
  val flags: List[String] = json.flags.as[List[String]]
  val breakdowns: List[Breakdown] = json.breakdowns.as[List[Breakdown]]

  // This allows us to define structural equality on the fields themselves
  private[SuccessResult] val fields =
    (resultId, adapterId, count, noiseTerms, i2b2Mapping, flags, breakdowns)

  override def equals(that: scala.Any): Boolean = that match {
    case sr: SuccessResult => sr.fields == fields
    case _ => false
  }

  override def hashCode(): Int = default.hash(fields)

  override def toString: String = s"SuccessResult$fields"
}

object SuccessResult {
  def unapply(arg: SuccessResult) = Some(arg.fields)
  def apply(json: Json): Option[SuccessResult] =
    if (JsonCompare.==(json.status, "success"))
      // This is shorter than doing a .is for every field.
      Try(new SuccessResult(json)).toOption
    else
      None
}

            // <--=== Pending Result ===--> //

final class PendingResult(val json: Json) extends QueryResult {
  override def equals(that: scala.Any): Boolean =
    that.isInstanceOf[PendingResult]

  override def toString: String = "PendingResult()"
}

object PendingResult {
  def apply(json: Json): Option[PendingResult] =
    if (JsonCompare.==(json.status, "status"))
      Some(new PendingResult(json))
    else None

}

            // <--=== Failure Result ===--> //

final class FailureResult(val json: Json) extends QueryResult {
  val problemDigest: ProblemDigest = json.problemDigest.as[ProblemDigest]

  override def equals(that: scala.Any): Boolean = that match {
    case fr: FailureResult => fr.problemDigest == problemDigest
    case _ => false
  }

  override def hashCode(): Int = problemDigest.hashCode

  override def toString: String = s"FailureResult($problemDigest)"
}

object FailureResult {
  def apply(json: Json): Option[FailureResult] =
    if (JsonCompare.==(json.status, "failure") && json.problemDigest.is[ProblemDigest])
      Some(new FailureResult(json))
    else None

  def unapply(arg: FailureResult): Option[ProblemDigest] =
    Some(arg.problemDigest)
}

// Just a private helper since I was using this three times
private object JsonCompare {
  def ==(json: Json, string: String): Boolean =
    json.is[String] && json.as[String].toLowerCase == string
}
