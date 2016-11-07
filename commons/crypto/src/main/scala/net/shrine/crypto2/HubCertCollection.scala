package net.shrine.crypto2

/**
  * Created by ty on 11/4/16.
  */
case class HubCertCollection(caEntry: KeyStoreEntry, downStreamAliases: Set[KeyStoreEntry], override val remoteSites: Seq[RemoteSite]) extends BouncyKeyStoreCollection {
  override val myEntry: KeyStoreEntry = caEntry

  override def allEntries: Iterable[KeyStoreEntry] = downStreamAliases + caEntry

  override def verifyBytes(signedBytes: Array[Byte], signatureBytes: Array[Byte]): Boolean =
    allEntries.exists(_.verify(signedBytes, signatureBytes))

}
