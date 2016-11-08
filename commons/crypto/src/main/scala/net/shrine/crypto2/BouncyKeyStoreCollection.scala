package net.shrine.crypto2


import java.io.{File, FileInputStream}
import java.security.cert.X509Certificate
import java.security.{KeyStore, PrivateKey, Security}

import net.shrine.crypto._
import net.shrine.log.Loggable
import net.shrine.util._
import org.bouncycastle.jce.provider.BouncyCastleProvider

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

  val provider = BouncyKeyStoreCollection.provider

  def signBytes(bytesToSign: Array[Byte]): Array[Byte] = myEntry.sign(bytesToSign).getOrElse(CryptoErrors.noKeyError(myEntry))

  def verifyBytes(cmsEncodedSignature: Array[Byte], originalMessage: Array[Byte]): Boolean

  def allEntries: Iterable[KeyStoreEntry]

  def remoteSites: Seq[RemoteSite]

  def keyStore: KeyStore = BouncyKeyStoreCollection.keyStore.getOrElse(throw new IllegalStateException("Accessing keyStore without loading from keyStore file first!"))

  def descriptor: KeyStoreDescriptor = BouncyKeyStoreCollection.descriptor.getOrElse(throw new IllegalStateException("Accessing keyStoreDescriptor without loading from keyStore file first!"))
}

/**
  * Factory object that reads the correct cert collection from the file.
  */
object BouncyKeyStoreCollection extends Loggable {
  import CryptoErrors._

  import scala.collection.JavaConversions._
  val provider = new BouncyCastleProvider()
  Security.addProvider(provider)
  var descriptor: Option[KeyStoreDescriptor] = None
  var keyStore: Option[KeyStore] = None
  val SHA256 = "SHA256withRSA"

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
        case SingleHubModel(isHub) => createHubCertCollection(entries, descriptor, isHub)
      }
  }

  def createHubCertCollection(entries: Set[KeyStoreEntry], descriptor: KeyStoreDescriptor, isHub: Boolean):
    EitherCertError =
  {
    if (entries.size != 2)
      Left(configureError(RequiresExactlyTwoEntries(entries)))
    else if (entries.count(_.privateKey.isDefined) != 1)
      Left(configureError(RequiresExactlyOnePrivateKey(entries.filter(_.privateKey.isDefined))))
    else {
      val partition    = entries.partition(_.privateKey.isDefined)
      val privateEntry = partition._1.head
      val caEntry      = partition._2.head
      val rsds = descriptor.remoteSiteDescriptors

      if (!isHub && rsds.head.keyStoreAlias.get != caEntry.aliases.first)
        Left(configureError(IncorrectAliasMapping(rsds.head.keyStoreAlias.get +: Nil, caEntry +: Nil)))
      else if (isHub && privateEntry.wasSignedBy(caEntry))
        Right(HubCertCollection(privateEntry, caEntry, rsds.map(rsd => RemoteSite(rsd.url, None, rsd.siteAlias, rsd.port))))
      else if (privateEntry.wasSignedBy(caEntry)) {
        val rsd = rsds.head
        Right(DownStreamCertCollection(privateEntry, caEntry, RemoteSite(rsd.url, Some(caEntry), rsd.siteAlias, rsd.port)))
      }
      else
        Left(configureError(NotSignedByCa(privateEntry +: Nil, caEntry)))
      }
  }

  def remoteDescriptorToRemoteSite(descriptor: KeyStoreDescriptor, entries: Set[KeyStoreEntry]): Seq[RemoteSite] = {
    descriptor.remoteSiteDescriptors.map(rsd => // Only safe with Peer/Downstream collections
      RemoteSite(rsd.url, entries.find(_.aliases.contains(rsd.keyStoreAlias.get)), rsd.siteAlias, rsd.port))
  }

  /**
    * @return a [[scala.util.Left]] if we can't find or disambiguate a [[PrivateKey]],
    *         otherwise return [[scala.util.Right]] that contains correct [[PeerCertCollection]]
    */
  def createPeerCertCollection(entries: Set[KeyStoreEntry], descriptor: KeyStoreDescriptor):
    EitherCertError =
  {
    val configKeyStoreAliases = descriptor.remoteSiteDescriptors.map(_.keyStoreAlias)

    if (configKeyStoreAliases.toSet.flatten != entries.map(_.aliases.first))
      Left(configureError(IncorrectAliasMapping(configKeyStoreAliases.flatten, entries)))
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