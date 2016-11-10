package net.shrine.crypto

/**
  * Created by ty on 10/27/16.
  */
case class PeerCertCollection(override val myEntry: KeyStoreEntry, entries: Set[KeyStoreEntry], override val remoteSites: Seq[RemoteSite]) extends BouncyKeyStoreCollection {

  def verifyBytes(cmsEncodedSignature: Array[Byte], originalMessage: Array[Byte]): Boolean = {
    (entries + myEntry).exists(_.verify(cmsEncodedSignature, originalMessage))
  }

  override val allEntries: Iterable[KeyStoreEntry] = entries + myEntry
}
