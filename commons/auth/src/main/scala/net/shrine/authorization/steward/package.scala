package net.shrine.authorization

/**
 * @author david 
 * @since 2/5/15
 */
package object steward {

  type StewardQueryId = Long
  type ExternalQueryId = Long
  type QueryContents = String
  type TopicId = Int
  type TopicStateName = String
  type Date = Long
  type UserName = String
  type Role = String

  val researcherRole = "Researcher"
  val stewardRole = "DataSteward"
  val qepRole = "qep"
}
