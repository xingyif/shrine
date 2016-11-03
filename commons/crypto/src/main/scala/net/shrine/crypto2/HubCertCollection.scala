package net.shrine.crypto2

/**
  * Created by ty on 10/25/16.
  */
case class HubCertCollection(override val myEntry: KeyStoreEntry, caEntry: KeyStoreEntry) extends BouncyKeyStoreCollection {

  override val allEntries: Iterable[KeyStoreEntry] = myEntry +: caEntry +: Nil

  /**
    * The only valid messages for a downstream node are those that come from the CA
    */
  override def verifyBytes(signedBytes:Array[Byte], signatureBytes:Array[Byte]) = caEntry.verify(signedBytes, signatureBytes)
}