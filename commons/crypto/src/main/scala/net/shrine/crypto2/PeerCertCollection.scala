package net.shrine.crypto2

/**
  * Created by ty on 10/27/16.
  */
case class PeerCertCollection(override val myEntry: KeyStoreEntry, entries: Set[KeyStoreEntry], override val remoteSites: Seq[RemoteSite]) extends BouncyKeyStoreCollection {

  def verifyBytes(signedBytes: Array[Byte], signatureBytes: Array[Byte]): Boolean = {
    (entries + myEntry).exists(_.verify(signedBytes, signatureBytes))
  }

  override val allEntries: Iterable[KeyStoreEntry] = entries + myEntry
}
