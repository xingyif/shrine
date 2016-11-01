package net.shrine.crypto2

import net.shrine.crypto.SigningCertStrategy
import net.shrine.log.Loggable
import net.shrine.protocol.BroadcastMessage

import scala.concurrent.duration.Duration

/**
  * Created by ty on 10/27/16.
  */
case class PeerCertCollection(override val myEntry: KeyStoreEntry, entries: Set[KeyStoreEntry]) extends BouncyKeyStoreCollection {

  def verifyBytes(signedBytes: Array[Byte], signatureBytes: Array[Byte]): Boolean = {
    (entries + myEntry).exists(_.verify(signedBytes, signatureBytes))
  }

  override val allEntries: Iterable[KeyStoreEntry] = entries + myEntry
}
