package net.shrine.protocol.query

import net.shrine.util.SEnum

/**
 * @author clint
 * @date Sep 22, 2014
 * 
 * An enum to represent allowable values for <panel_timing> elements in
 * i2b2 XML blobs.
 */
final case class PanelTiming private (name: String) extends PanelTiming.Value {
  def isAny: Boolean = this eq PanelTiming.Any
}

object PanelTiming extends SEnum[PanelTiming] {
  val Any = PanelTiming("ANY")
  val SameVisit = PanelTiming("SAMEVISIT")
  val SameInstanceNum = PanelTiming("SAMEINSTANCENUM")
}