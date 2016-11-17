package net.shrine.crypto

import javax.naming.ConfigurationException

import net.shrine.log.Log
import net.shrine.problem.{AbstractProblem, ProblemSources}
import net.shrine.protocol.BroadcastMessage

/**
  * Created by ty on 10/27/16.
  */
case class ImproperlyConfiguredKeyStoreProblem(override val throwable: Option[Throwable], override val description: String)
  extends AbstractProblem(ProblemSources.Commons)
{
  override val summary: String = "There is a problem with how the KeyStore has been configured"
}

case class ImproperlyConfiguredKeyStoreException(message: String) extends ConfigurationException(message)

object CryptoErrors {
  type Entries = Iterable[KeyStoreEntry]

  def comma(entries: Entries): String = entries.flatMap(_.aliases).mkString(", ")

  final val NoPrivateKeyInStore =
    "Could not find a key in the KeyStore with a PrivateKey. Without one, SHRINE cannot sign messages."
  final val CouldNotFindCa =
    "You must specify at least one ca cert alias corresponding to a PrivateKey entry for the Hub in a non PeerToPeer network"
  final def TooManyPrivateKeys(entries: Entries)  =
    s"There are ${entries.size} entries in the KeyStore with a PrivateKey. Please specify which one to use for signing queries under `privateKeyAlias` in the configuration file."
  final def CouldNotFindAlias(alias: String) =
    s"Could not find a KeyStore Entry corresponding to the alias '$alias'"
  final def CouldNotFindCaAlias(entries: Entries) =
    s"Could not find a KeyStore Entry corresponding to the aliases '${comma(entries)}"
  final def NotSignedByCa(entries: Entries, caEntry: KeyStoreEntry) =
    s"The private entries `${comma(entries)}` were not signed by the ca entry `${caEntry.aliases.first}`"
  final def RequiresExactlyTwoEntries(entries: Entries) =
    s"Hub based networks require exactly two entries in the KeyStore, found ${entries.size} entries: `${comma(entries)}`"
  final def RequiresExactlyOnePrivateKey(entries: Entries) =
    s"Hub based networks require exactly one private key entry in the KeyStore, found ${entries.size} with private keys: `${comma(entries)}"
  final def PrivateEntryIsCaEntry(aliases: Iterable[String]) =
    s"Your private cert must not also be your CA cert. Intersecting aliases: `${aliases.mkString(", ")}`"
  final def ExpiredCertificates(entries: Entries) =
    s"The following certificates have expired: `${comma(entries)}`"
  final def IncorrectAliasMapping(aliases: Iterable[String], entries: Entries) =
    s"The remote site aliases did not correspond to the aliases in the KeyStore.\n  Remote Site aliases: `${aliases.mkString(", ")}`\n  KeyStore Aliases: `${comma(entries)}`"

  private[crypto] def noKeyError(myEntry: KeyStoreEntry) = {
    val illegalEntry = new IllegalArgumentException(s"The provided keystore entry $myEntry did not have a private key")
    val problem = ImproperlyConfiguredKeyStoreProblem(Some(illegalEntry),
      s"The KeyStore entry identified as the signing cert for this node did not provide a private key to sign with." +
        s" Please check the KeyStore entry with the alias `${myEntry.aliases.first}`.")
    throw problem.throwable.get
  }

  private[crypto] def invalidSiganatureFormat(bytes: Array[Byte]) = {
    val illegalSignature = new IllegalArgumentException("Given a signature with bytes that are not valid CMSSignedData")
    val problem = InvalidSignatureFormatProblem(bytes, Some(illegalSignature))
    throw problem.throwable.get
  }

  private[crypto] def configureError(description: String): ImproperlyConfiguredKeyStoreProblem = {
    ImproperlyConfiguredKeyStoreProblem(Some(ImproperlyConfiguredKeyStoreException(description)), description)
  }
}

case class UnknownSignatureProblem(message: BroadcastMessage)
  extends AbstractProblem(ProblemSources.Commons)
{
  override def summary: String = s"Could not find a matching certificate for the received message"

  override def description: String = {
    s"The message with id '${message.requestId}' contained an unknown signature. Please " +
    "check that the KeyStore is properly configured, and that SHRINE is on the same version."
  }
}

case class InvalidSignatureFormatProblem(illegalBytes: Array[Byte], override val throwable: Option[Throwable])
  extends AbstractProblem(ProblemSources.Commons)
{
  override def summary: String = s"An incoming message contained a signature that was not signed using CMSSignedData."

  override def description: String =
    s"The message with signature bytes ${s"[${illegalBytes.mkString(" ")}]"} was not signed using CMSSignedData, which means" +
      s"it's in an invalid format. Please check that every SHRINE node is operating on the same version."
}
