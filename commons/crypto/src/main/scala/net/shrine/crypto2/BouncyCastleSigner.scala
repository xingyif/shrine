package net.shrine.crypto2

import net.shrine.crypto.{Signer, SigningCertStrategy}
import net.shrine.protocol.{BroadcastMessage, Signature}

/**
  * Created by ty on 10/25/16.
  */
object BouncyCastleSigner {
  def sign(message: BroadcastMessage, signingCertStrategy: SigningCertStrategy): BroadcastMessage = {
    message.copy(signature = Option(generateSignature))
  }

  def generateSignature: Signature = ???
}
