package net.shrine.broadcaster

import net.shrine.crypto.Signer
import net.shrine.aggregation.Aggregator
import net.shrine.protocol.BroadcastMessage
import scala.concurrent.Future
import net.shrine.protocol.BaseShrineResponse
import net.shrine.crypto.SigningCertStrategy
import net.shrine.broadcaster.dao.HubDao

/**
 * @author clint
 * @date Feb 28, 2014
 */
final case class SigningBroadcastAndAggregationService(broadcasterClient: BroadcasterClient, signer: Signer, signingCertStrategy: SigningCertStrategy) extends AbstractBroadcastAndAggregationService(broadcasterClient, signer.sign(_, signingCertStrategy))