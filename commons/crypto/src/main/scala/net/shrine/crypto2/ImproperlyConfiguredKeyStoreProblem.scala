package net.shrine.crypto2

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
  final val NoPrivateKeyInStore = "Could not find a key in the KeyStore with a PrivateKey. Without one, SHRINE cannot sign mesasges."
  final val TooManyPrivateKeys  = "There are multiple entries in the KeyStore with a PrivateKey. Please specify which one to use for signing queries under `privateKeyAlias` in the configuration file."
  final def CouldNotFindAlias(alias: String) = s"Could not find a KeyStore Entry corresponding to the alias '$alias'"
  final def CouldNotFindCaAlias(aliases: Seq[String]) = s"Could not find a KeyStore Entry corresponding to the aliases '${aliases.mkString(", ")}"
  final def NotSignedByCa(aliases: Set[String], caAlias: String) = s"The KeyStore Entries with aliases `${aliases.mkString(", ")}` were not signed by the ca `$caAlias`"

  private[crypto2] def noKeyError(myEntry: KeyStoreEntry) = {
    val illegalEntry = new IllegalArgumentException(s"The provided keystore entry $myEntry did not have a private key")
    Log.error(ImproperlyConfiguredKeyStoreProblem(Some(illegalEntry),
      s"The KeyStore entry identified as the signing cert for this node did not provide a private key to sign with." +
        s" Please check the KeyStore entry with the alias `${myEntry.aliases.first}`.").toDigest)
    throw illegalEntry
  }

  private[crypto2] def configureError(description: String): ImproperlyConfiguredKeyStoreProblem = {
    val err = ImproperlyConfiguredKeyStoreProblem(Some(ImproperlyConfiguredKeyStoreException(description)), description)
    Log.error(err.toDigest)
    err
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
