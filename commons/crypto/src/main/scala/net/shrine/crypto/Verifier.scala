package net.shrine.crypto

import net.shrine.protocol.BroadcastMessage
import scala.concurrent.duration.Duration

/**
 * @author clint
 * @date Nov 27, 2013
 */

//todo delete this interface . Only one thing implements it.
trait Verifier {
  def verifySig(message: BroadcastMessage, maxSignatureAge: Duration): Boolean
}