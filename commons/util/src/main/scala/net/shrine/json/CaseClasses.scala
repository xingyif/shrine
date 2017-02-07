package net.shrine.json

import java.util.UUID

// The Adapter, Topic, and User entities don't have to
// carry arbitrary json, so they can just be case classes
case class Adapter(name: String, id: UUID)
case class Topic(name: String, description: String, id: UUID)
case class User(userName: String, domain: String, id: UUID)

case class Breakdown(category: String, results: List[BreakdownProperty])

case class BreakdownProperty(name: String, count: Int)
case class NoiseTerms(clamp: Int, sigma: Int, rounding: Int)
