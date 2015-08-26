package net.shrine.protocol.query

import net.shrine.util.SEnum

/**
 * @author clint
 * @date Sep 22, 2014
 * 
 * An enum to represent allowable values for <query_timing> elements in
 * i2b2 XML blobs.
 */
final case class QueryTiming private (name: String) extends QueryTiming.Value {
  def isAny: Boolean = this == QueryTiming.Any
}

object QueryTiming extends SEnum[QueryTiming] {
  val Any = QueryTiming("ANY")
  val SameVisit = QueryTiming("SAMEVISIT")
  val SameInstanceNum = QueryTiming("SAMEINSTANCENUM")
  
  lazy val notAny: Seq[QueryTiming] = Seq(SameVisit, SameInstanceNum)
}