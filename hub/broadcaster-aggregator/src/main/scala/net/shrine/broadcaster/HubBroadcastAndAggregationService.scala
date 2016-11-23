package net.shrine.broadcaster

import java.net.URL

/**
 * @author clint
 * @date Nov 15, 2013
 */
final case class HubBroadcastAndAggregationService(broadcasterClient: BroadcasterClient, override val broadcasterUrl: Option[URL] = None) extends AbstractBroadcastAndAggregationService(broadcasterClient) {
  override def attachSigningCert: Boolean = false
}