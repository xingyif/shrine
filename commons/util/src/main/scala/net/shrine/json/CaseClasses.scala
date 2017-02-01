package net.shrine.json

import java.util.UUID

// The Adapter, Topic, and User entities don't have to
// carry arbitrary json, so they can just be case classes
case class Adapter(name: String, id: UUID)
case class Topic(name: String, description: String, id: UUID)
case class User(userName: String, domain: String, id: UUID)

// The Breakdown type used for Success Results
// TODO: Do we really gain anything by having these as four
// TODO: separate case classes? Could just have a Breakdown
// TODO: class with an enum field
object Breakdown {
  def fromStringResults(string: String, results: List[BreakdownProperty]): Option[Breakdown] = string.toLowerCase match {
    case "gender" => Some(GenderBreakdown(results))
    case "age" => Some(AgeBreakdown(results))
    case "race" => Some(RaceBreakdown(results))
    case "vital status" => Some(VitalStatusBreakdown(results))
    case _ => None
  }
}

sealed trait Breakdown {
  val results: List[BreakdownProperty]
}

case class GenderBreakdown(results: List[BreakdownProperty]) extends Breakdown
case class AgeBreakdown(results: List[BreakdownProperty]) extends Breakdown
case class RaceBreakdown(results: List[BreakdownProperty]) extends Breakdown
case class VitalStatusBreakdown(results: List[BreakdownProperty]) extends Breakdown

case class BreakdownProperty(name: String, count: Int)
case class NoiseTerms(clamp: Int, sigma: Int, rounding: Int)
