package net.shrine.broadcaster

import net.shrine.log.Loggable

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import net.shrine.aggregation.Aggregator
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.FailureResult$
import net.shrine.protocol.Result
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.SingleNodeResult
import net.shrine.protocol.Timeout
import net.shrine.util.XmlDateHelper
import net.shrine.crypto.Signer
import net.shrine.protocol.BaseShrineResponse
import net.shrine.broadcaster.dao.HubDao

/**
 * @author clint
 * @date Nov 15, 2013
 */
final case class HubBroadcastAndAggregationService(broadcasterClient: BroadcasterClient) extends AbstractBroadcastAndAggregationService(broadcasterClient) {
  override def attachSigningCert: Boolean = false
}