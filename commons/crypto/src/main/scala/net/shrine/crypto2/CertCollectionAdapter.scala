package net.shrine.crypto2

import java.security.{KeyStore, Principal}
import java.security.cert.X509Certificate

import net.shrine.crypto.{CertCollection, KeyPair, KeyStoreDescriptor}
import net.shrine.protocol.CertId


/**
  * Allows gradual replacement of the old crypto package by keeping the old
  * interface for now
  */
final case class CertCollectionAdapter(keyStoreCollection: BouncyKeyStoreCollection)
  extends BouncyKeyStoreCollection with CertCollection
{
  override def signBytes(bytesToSign: Array[Byte]): Array[Byte] = keyStoreCollection.signBytes(bytesToSign)

  override def verifyBytes(cmsEncodedSignature: Array[Byte], originalMessage: Array[Byte]): Boolean = keyStoreCollection.verifyBytes(cmsEncodedSignature, originalMessage)

  override val myEntry: KeyStoreEntry = keyStoreCollection.myEntry

  override def allEntries: Iterable[KeyStoreEntry] = keyStoreCollection.allEntries

  override def remoteSites: Seq[RemoteSite] = keyStoreCollection.remoteSites

  override def myCertId: Option[CertId] = Some(entryToCertId(myEntry))

  override def myCert: Option[X509Certificate] = Some(myEntry.cert)

  override def caCertAliases: Seq[String] = caEntry.aliases

  override def caCerts: Map[Principal, X509Certificate] = Map(caEntry.cert.getIssuerDN -> caEntry.cert)

  override def myKeyPair: KeyPair = KeyPair(myEntry.publicKey, myEntry.privateKey.get)

  override def get(id: CertId): Option[X509Certificate] = certIdsToCerts.get(id)

  override def iterator: Iterator[X509Certificate] = keyStoreCollection.allEntries.map(_.cert).iterator

  override def ids: Iterable[CertId] = allEntries.filterNot(_ == caEntry).map(entryToCertId)

  override def caIds: Iterable[CertId] = Seq(entryToCertId(caEntry))

  // CertIds are just the serial number along with the alias
  private def entryToCertId(keyStoreEntry: KeyStoreEntry): CertId =
    CertId(keyStoreEntry.certificateHolder.getSerialNumber, Some(keyStoreEntry.aliases.first))

  private val certIdsToCerts: Map[CertId, X509Certificate] = keyStoreCollection.allEntries.map(entry => entryToCertId(entry) -> entry.cert).toMap

  // The CertCollection doesn't really account for PeerToPeer networks, so we slightly ignore that too
  private val caEntry: KeyStoreEntry = keyStoreCollection match {
    case DownStreamCertCollection(_, ca, _)     => ca
    case HubCertCollection(ca, _, _)            => ca
    case PeerCertCollection(privateEntry, _, _) => privateEntry
  }

}
