package net.shrine.crypto

import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.Signature

/**
 * @author clint
 * @date Nov 25, 2013
 */
trait Signer {
  def sign(message: BroadcastMessage, signingCertStrategy: SigningCertStrategy): BroadcastMessage
}