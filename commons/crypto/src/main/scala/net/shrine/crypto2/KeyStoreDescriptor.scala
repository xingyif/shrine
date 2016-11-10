package net.shrine.crypto2

import net.shrine.util.{SingleHubModel, TrustModel}

/**
 * @author clint
 * @since Nov 22, 2013
 */

//todo consolidate with KeyStoreParser, maybe combine the whole works into KeyStoreCertCollection's collection
final case class KeyStoreDescriptor(
    file: String,
    password: String,
    privateKeyAlias: Option[String],
    caCertAliases: Seq[String],
    trustModel: TrustModel,
    remoteSiteDescriptors: Seq[RemoteSiteDescriptor],
    keyStoreType: KeyStoreType = KeyStoreType.Default) // TODO: make this non-default once crypto is deleted
{
  override def toString = scala.runtime.ScalaRunTime._toString(this.copy(password = "REDACTED"))
}