package net.shrine.crypto2

import java.security.{KeyStore, Principal}
import java.security.cert.X509Certificate

import net.shrine.crypto.{CertCollection, KeyPair, KeyStoreDescriptor}
import net.shrine.log.Loggable
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.CertId
import net.shrine.util.NonEmptySeq

/**
  * Created by ty on 10/25/16.
  */
case class DownstreamKeyStoreCollection(myEntry: KeyStoreEntry, caEntry: KeyStoreEntry) extends CertCollection with Loggable {

  val privateKey = myEntry.privateKey.getOrElse(noKeyError)

  /**
    * The only valid messages for a downstream node are those that come from the CA
    */
  def verify = caEntry.verify _

  def sign = myEntry.sign _

  override def myCertId: Option[CertId] = Some(myEntry).map(entryToCertID)

  override def myCert: Option[X509Certificate] = Some(myEntry.cert)

  override def caCertAliases: NonEmptySeq[String] = caEntry.aliases

  override def caCerts: Map[Principal, X509Certificate] = Map(caEntry.cert.getSubjectDN -> caEntry.cert)

  override def myKeyPair: KeyPair = KeyPair(myEntry.publicKey, privateKey)

  override def get(id: CertId): Option[X509Certificate] = Seq(myEntry, caEntry).find(entryToCertID(_).serial == id.serial).map(_.cert)

  override def iterator: Iterator[X509Certificate] = Seq(myEntry, caEntry).map(_.cert).iterator

  private[this] def id(entry: KeyStoreEntry) = Seq(entry).map(entryToCertID)

  override def ids: Iterable[CertId] = id(myEntry)

  override def caIds: Iterable[CertId] = id(caEntry)

  private[this] def noKeyError = {
    val illegalEntry = new IllegalArgumentException(s"The provided keystore entry $myEntry did not have a private key")
    error(ImproperlyConfiguredKeyStoreProblem(Some(illegalEntry),
      "The KeyStore entry identified as the signing cert for this node did not provide a private key to sign with." +
        s" Please check the KeyStore entry with the alias \"${myEntry.aliases.first}\"."))
    throw illegalEntry
  }

  private[this] def entryToCertID(entry: KeyStoreEntry): CertId = {
    CertId(entry.certificateHolder.getSerialNumber, Option(entry.certificateHolder.getSubject.toString))
  }
}