package net.shrine.crypto2


import java.math.BigInteger
import java.security.cert.X509Certificate
import java.security.{KeyStore, PrivateKey}
import javax.xml.datatype.XMLGregorianCalendar

import net.shrine.crypto._
import net.shrine.log.Loggable
import net.shrine.protocol.{BroadcastMessage, CertId, Signature}
import net.shrine.util._

import scala.concurrent.duration.Duration

/**
  * Created by ty on 10/25/16.
  */
trait BouncyKeyStoreCollection extends Iterable[KeyStoreEntry] with Signer with Verifier with Loggable {

  def sign(bytesToSign: Array[Byte]): Option[Array[Byte]]

  def verify(signedBytes: Array[Byte], signatureBytes: Array[Byte]): Boolean

  val myEntry: KeyStoreEntry

  override def verifySig(message: BroadcastMessage, maxSignatureAge: Duration): Boolean = {
    val logSigFailure = (b:Boolean) => {
      if (!b) {
        UnknownSignatureProblem(message)
        warn(s"Error verifying signature for message with id '${message.requestId}'")
      }
      b
    }

    message.signature.exists(sig =>
      notTooOld(sig, maxSignatureAge, message) && logSigFailure(verify(toBytes(message, sig.timestamp), sig.value.array))
    )
  }

  override def sign(message: BroadcastMessage, signingCertStrategy: SigningCertStrategy): BroadcastMessage = {
    val timeStamp = XmlDateHelper.now
    val dummyCertId = CertId(BigInteger.valueOf(10l), None)
    val signedBytes = sign(toBytes(message, timeStamp)).getOrElse(CryptoErrors.noKeyError(myEntry))
    val sig = Signature(timeStamp, dummyCertId, None, signedBytes)
    message.withSignature(sig)
  }

  private def toBytes(message: BroadcastMessage, timestamp: XMLGregorianCalendar): Array[Byte] = {
    val messageXml = message.copy(signature = None).toXmlString
    val timestampXml = timestamp.toXMLFormat

    (messageXml + timestampXml).getBytes("UTF-8")
  }

  private def notTooOld(sig: Signature, maxSignatureAge: Duration, message: BroadcastMessage): Boolean = {
    import XmlGcEnrichments._

    val sigValidityEndTime: XMLGregorianCalendar = sig.timestamp + maxSignatureAge
    val now = XmlDateHelper.now
    val timeout = sigValidityEndTime > now

    if (timeout) warn(s"Could not validate message with id '${message.requestId}' due to " +
      s"exceeding max timeout of $maxSignatureAge")

    timeout
  }
}

/**
  * Factory object that reads the correct cert collection from the file.
  */
object BouncyKeyStoreCollection extends Loggable {
  import scala.collection.JavaConversions._
  type EitherCertError = Either[ImproperlyConfiguredKeyStoreProblem, BouncyKeyStoreCollection]

  def createCertCollection(keyStore: KeyStore, descriptor: KeyStoreDescriptor):
    EitherCertError =
  {
    // Read all of the KeyStore entries from the file into a KeyStore Entry
    val values = keyStore.aliases().map(alias =>
      (alias, keyStore.getCertificate(alias), Option(keyStore.getKey(alias, descriptor.password.toCharArray).asInstanceOf[PrivateKey])))
    val entries = values.map(value => KeyStoreEntry(value._2.asInstanceOf[X509Certificate], NonEmptySeq(value._1, Nil), value._3)).toSet
    descriptor.trustModel match {
      case PeerToPeerModel => createHubCertCollection(entries, descriptor) // In a p2p network, no need to check that everything is signed by CA
      case SingleHubModel  => for {
        caEntry <- entries.find(_.aliases.intersect(descriptor.caCertAliases).nonEmpty)
                          .toRight(CryptoErrors.configureError(CryptoErrors.CouldNotFindCaAlias(descriptor.caCertAliases))).right
        signedByCa <- chooseDownStreamOrHub(caEntry, entries, descriptor).right
      } yield signedByCa
    }
  }

  def createHubCertCollection(entries: Set[KeyStoreEntry], descriptor: KeyStoreDescriptor): EitherCertError = {
    getPrivateKeyEntry(entries, descriptor) match {
      case Left(p)      => Left(p)
      case Right(entry) => Right(HubKeyStoreCollection(entry, (entries - entry).toSeq))
    }
  }

  // Ensures that every cert has been signed by the CaEntry, and then determines whether to create a
  // Hub collection or a Downstream collection by whether or not the CaEntry also contains the private key.
  def chooseDownStreamOrHub(caEntry: KeyStoreEntry, entries: Set[KeyStoreEntry], descriptor: KeyStoreDescriptor): EitherCertError = {
    val allSignedByCa = entries.forall(_.wasSignedBy(caEntry))
    (allSignedByCa, getPrivateKeyEntry(entries, descriptor)) match {
      case(_, Left(problem)) => 
        Left(problem)
      case(false, _) => 
        val notSigned = entries.filterNot(_.wasSignedBy(caEntry)).map(_.aliases.first)
        val caAlias = caEntry.aliases.first
        Left(CryptoErrors.configureError(CryptoErrors.NotSignedByCa(notSigned, caAlias)))
      case(true, Right(privateEntry)) if caEntry == privateEntry =>
        Right(HubKeyStoreCollection(caEntry, (entries - caEntry).toSeq))
      case(true, Right(privateEntry)) =>
        Right(DownstreamKeyStoreCollection(privateEntry, caEntry))
    }
  }

  // Returns the private key, or an error describing why the private key couldn't be ascertained
  def getPrivateKeyEntry(entries: Set[KeyStoreEntry], descriptor: KeyStoreDescriptor): Either[ImproperlyConfiguredKeyStoreProblem, KeyStoreEntry] = {
    val entryKeys = entries.filter(_.privateKey.isDefined)
    descriptor.privateKeyAlias match {
      case Some(alias) =>
        entryKeys.find(_.aliases.first == alias).toRight(CryptoErrors.configureError(CryptoErrors.CouldNotFindAlias(alias)))
      case None        =>
        if (entryKeys.size == 1) {
          val entry = entryKeys.head
          info(s"Found one cert with a private key, with alias '${entry.aliases.first}'")
          Right(entry)
        } else Left(CryptoErrors.configureError(CryptoErrors.TooManyPrivateKeys))
    }
  }

  //TODO: Move fromFileRecoverWithClassPath to crypto2
  def fromFileRecoverWithClassPath(descriptor: KeyStoreDescriptor): BouncyKeyStoreCollection = {
    createCertCollection(KeyStoreCertCollection.fromFileRecoverWithClassPath(descriptor).keystore, descriptor)
      .fold(problem => throw problem.throwable.get, identity)
  }
}