package net.shrine.crypto2


import java.io.{File, FileInputStream}
import java.math.BigInteger
import java.security.cert.X509Certificate
import java.security.{KeyStore, PrivateKey, Security}
import java.time.Instant
import java.util.Date
import javax.xml.datatype.XMLGregorianCalendar

import net.shrine.crypto._
import net.shrine.log.Loggable
import net.shrine.protocol.{BroadcastMessage, CertId, Signature}
import net.shrine.util._
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.jce.provider.BouncyCastleProvider

import scala.concurrent.duration.Duration

/**
  * Created by ty on 10/25/16.
  *
  * Rewrite of [[net.shrine.crypto.CertCollection]]. Abstracts away the need to track down
  * all the corresponding pieces of a KeyStore entry by collecting them into a collection
  * of [[KeyStoreEntry]]s.
  * See: [[DownStreamCertCollection]], [[PeerCertCollection]], [[CertCollectionAdapter]]
  */
trait BouncyKeyStoreCollection extends Loggable {

  val myEntry: KeyStoreEntry

  def signBytes(bytesToSign: Array[Byte]): Array[Byte] = myEntry.sign(bytesToSign).getOrElse(CryptoErrors.noKeyError(myEntry))

  def verifyBytes(signedBytes: Array[Byte], signatureBytes: Array[Byte]): Boolean

  def allEntries: Iterable[KeyStoreEntry]

  def remoteSites: Seq[RemoteSite]

  def keyStore: KeyStore = BouncyKeyStoreCollection.keyStore.getOrElse(throw new IllegalStateException("Accessing keyStore without loading from keyStore file first!"))

  def descriptor: KeyStoreDescriptor = BouncyKeyStoreCollection.descriptor.getOrElse(throw new IllegalStateException("Accessing keyStoreDescriptor without loading from keyStore file first!"))
}

/**
  * Factory object that reads the correct cert collection from the file.
  */
object BouncyKeyStoreCollection extends Loggable {
  import scala.collection.JavaConversions._
  import CryptoErrors._
  Security.addProvider(new BouncyCastleProvider())
  var descriptor: Option[KeyStoreDescriptor] = None
  var keyStore: Option[KeyStore] = None

  // On failure creates a problem so it gets logged into the database.
  type EitherCertError = Either[ImproperlyConfiguredKeyStoreProblem, BouncyKeyStoreCollection]

  /**
    * Creates a cert collection from a keyStore. Returns an Either to abstract away
    * try catches/problem construction until the end.
    * @return [[EitherCertError]]
    */
  def createCertCollection(keyStore: KeyStore, descriptor: KeyStoreDescriptor):
    EitherCertError =
  {
    BouncyKeyStoreCollection.descriptor = Some(descriptor)
    BouncyKeyStoreCollection.keyStore = Some(keyStore)
    // Read all of the KeyStore entries from the file into a KeyStore Entry
    val values = keyStore.aliases().map(alias =>
      (alias, keyStore.getCertificate(alias), Option(keyStore.getKey(alias, descriptor.password.toCharArray).asInstanceOf[PrivateKey])))
    val entries = values.map(value => KeyStoreEntry(value._2.asInstanceOf[X509Certificate], NonEmptySeq(value._1, Nil), value._3)).toSet
    if (entries.exists(_.isExpired()))
      Left(configureError(ExpiredCertificates(entries.filter(_.isExpired()))))
    else
      descriptor.trustModel match {
        case PeerToPeerModel       => createPeerCertCollection(entries, descriptor)
        case SingleHubModel(false) => createDownStreamCollection(entries, descriptor)
        case SingleHubModel(true)  => createHubCertCollection(entries, descriptor)
      }
  }

  def createHubCertCollection(entries: Set[KeyStoreEntry], descriptor: KeyStoreDescriptor):
    EitherCertError =
  {
    if (descriptor.privateKeyAlias.isDefined)
      warn(s"Specifying the private key alias for the Hub in a non PeerToPeer network is useless, as it uses caCertAliases.")
    val configKeyStoreAliases = descriptor.remoteSiteDescriptors.map(_.keyStoreAlias)

    if (configKeyStoreAliases.toSet != entries.map(_.aliases.first))
      Left(configureError(IncorrectAliasMapping(configKeyStoreAliases, entries)))
    else
    (descriptor.caCertAliases, entries.filter(_.privateKey.isDefined)) match {
      case (certs, keys) if certs.isEmpty || keys.isEmpty => Left(configureError(CouldNotFindCa))
      case (_, keys) if keys.size > 1 => Left(configureError(TooManyPrivateKeys(keys)))
      case (certs, key) if key.head.aliases.intersect(certs).isEmpty => Left(configureError(CouldNotFindCa))
      case (certs, key) =>
        val caEntry = key.head
        val unsignedEntries = (entries - caEntry).filter(!_.wasSignedBy(caEntry))

        if (unsignedEntries.nonEmpty)
          Left(configureError(NotSignedByCa(unsignedEntries, caEntry)))
        else
          Right(HubCertCollection(caEntry, entries - caEntry, remoteDescriptorToRemoteSite(descriptor, entries)))

    }

  }

  def remoteDescriptorToRemoteSite(descriptor: KeyStoreDescriptor, entries: Set[KeyStoreEntry]): Seq[RemoteSite] = {
    descriptor.remoteSiteDescriptors.map(rsd =>
      RemoteSite(rsd.url, entries.find(_.aliases.contains(rsd.keyStoreAlias)).get, rsd.siteAlias))
  }

  /**
    * @return a [[scala.util.Left]] if we can't find or disambiguate a [[PrivateKey]],
    *         otherwise return [[scala.util.Right]] that contains correct [[PeerCertCollection]]
    */
  def createPeerCertCollection(entries: Set[KeyStoreEntry], descriptor: KeyStoreDescriptor):
    EitherCertError =
  {
    if (descriptor.caCertAliases.nonEmpty)
      warn(s"Specifying caCertAliases in a PeerToPeer network is useless, certs found: `${descriptor.caCertAliases}`")
    val configKeyStoreAliases = descriptor.remoteSiteDescriptors.map(_.keyStoreAlias)

    if (configKeyStoreAliases.toSet != entries.map(_.aliases.first))
      Left(configureError(IncorrectAliasMapping(configKeyStoreAliases, entries)))
    else
      (descriptor.privateKeyAlias, entries.filter(_.privateKey.isDefined)) match {
      case (_, empty) if empty.isEmpty => Left(configureError(NoPrivateKeyInStore))
      case (None, keys) if keys.size == 1 =>
        warn(s"No private key specified, using the only entry with a private key: `${keys.head.aliases.first}`")
        Right(PeerCertCollection(keys.head, entries -- keys,
          remoteDescriptorToRemoteSite(descriptor, entries)))
      case (None, keys)                => Left(configureError(TooManyPrivateKeys(entries)))
      case (Some(alias), keys) if keys.exists(_.aliases.contains(alias)) =>
        val privateKeyEntry = keys.find(_.aliases.contains(alias)).get
        Right(PeerCertCollection(privateKeyEntry, entries - privateKeyEntry,
          remoteDescriptorToRemoteSite(descriptor, entries)))
      case (Some(alias), keys)         => Left(configureError(CouldNotFindAlias(alias)))
    }
  }

  def createDownStreamCollection(entries: Set[KeyStoreEntry], descriptor: KeyStoreDescriptor):
    EitherCertError =
  {
    if (descriptor.caCertAliases.nonEmpty)
      warn(s"Specifying caCertAliases for a DownStream node is useless, as you write the cert in aliasMap anyways. Certs found: `${descriptor.caCertAliases}`")
    if (entries.size != 2)
      Left(configureError(RequiresExactlyTwoEntries(entries)))
    else if (entries.count(_.privateKey.isDefined) != 1)
      Left(configureError(RequiresExactlyOnePrivateKey(entries.filter(_.privateKey.isDefined))))
    else {
      val partition    = entries.partition(_.privateKey.isDefined)
      val privateEntry = partition._1.head
      val caEntry      = partition._2.head
      val rsd          = descriptor.remoteSiteDescriptors.head

      if (descriptor.remoteSiteDescriptors.head.keyStoreAlias != caEntry.aliases.first)
        Left(configureError(IncorrectAliasMapping(rsd.keyStoreAlias +: Nil, caEntry +: Nil)))
      else if (privateEntry.wasSignedBy(caEntry))
        Right(DownStreamCertCollection(privateEntry, caEntry, RemoteSite(rsd.url, caEntry, rsd.siteAlias)))
      else
        Left(configureError(NotSignedByCa(privateEntry +: Nil, caEntry)))
    }
  }


  //TODO: Move fromStreamHelper to crypto2
  def fromFileRecoverWithClassPath(descriptor: KeyStoreDescriptor): BouncyKeyStoreCollection = {
    val keyStore =
      if (new File(descriptor.file).exists)
        KeyStoreCertCollection.fromStreamHelper(descriptor, new FileInputStream(_))
      else
        KeyStoreCertCollection.fromStreamHelper(descriptor, getClass.getClassLoader.getResourceAsStream(_))

    BouncyKeyStoreCollection.keyStore = Some(keyStore)
    BouncyKeyStoreCollection.descriptor = Some(descriptor)

    createCertCollection(keyStore, descriptor)
      .fold(problem => throw problem.throwable.get, identity)
  }
}