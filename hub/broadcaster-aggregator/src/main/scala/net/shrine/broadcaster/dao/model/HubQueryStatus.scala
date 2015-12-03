package net.shrine.broadcaster.dao.model

import net.shrine.util.SEnum

/**
 * @author clint
 * @date Dec 17, 2014
 */
final case class HubQueryStatus(name: String) extends HubQueryStatus.Value

object HubQueryStatus extends SEnum[HubQueryStatus] {
  val Success = HubQueryStatus("SUCCESS")
  val Failure = HubQueryStatus("FAILURE")
  val Timeout = HubQueryStatus("TIMEOUT")
  val DownstreamFailure = HubQueryStatus("DOWNSTREAM_FAILURE")
  val Unknown = HubQueryStatus("UNKNOWN")
}