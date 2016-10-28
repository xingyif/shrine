package net.shrine.crypto2

/**
  * Created by ty on 10/25/16.
  */
case class DownstreamKeyStoreCollection(override val myEntry: KeyStoreEntry, caEntry: KeyStoreEntry) extends BouncyKeyStoreCollection {

  override def iterator: Iterator[KeyStoreEntry] = Seq(myEntry, caEntry).iterator

  override def sign(bytesToSign: Array[Byte]): Option[Array[Byte]] = myEntry.sign(bytesToSign)

  /**
    * The only valid messages for a downstream node are those that come from the CA
    */
  override def verify(signedBytes:Array[Byte], signatureBytes:Array[Byte]) = {caEntry.verify(signedBytes, signatureBytes)}
}