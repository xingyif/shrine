package net.shrine.crypto2

import net.shrine.protocol.BroadcastMessage

import scala.concurrent.duration.Duration

/**
 * @author clint
 * @date Nov 27, 2013
 */

trait Verifier {
  def verifySig(message: BroadcastMessage, maxSignatureAge: Duration): Boolean
}