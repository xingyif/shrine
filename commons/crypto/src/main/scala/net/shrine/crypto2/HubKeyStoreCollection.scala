package net.shrine.crypto2

import net.shrine.crypto.SigningCertStrategy
import net.shrine.log.Loggable
import net.shrine.protocol.BroadcastMessage

import scala.concurrent.duration.Duration

/**
  * Created by ty on 10/27/16.
  */
case class HubKeyStoreCollection(override val myEntry: KeyStoreEntry, entries: Seq[KeyStoreEntry]) extends BouncyKeyStoreCollection {

  def sign(bytesToSign: Array[Byte]) = myEntry.sign(bytesToSign)

  def verify(signedBytes: Array[Byte], signatureBytes: Array[Byte]): Boolean = {
    (myEntry +: entries).exists(_.verify(signedBytes, signatureBytes))
  }

  override def iterator: Iterator[KeyStoreEntry] = (myEntry +: entries).iterator
}
