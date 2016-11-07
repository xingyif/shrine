package net.shrine.crypto
import net.shrine.util.{PeerToPeerModel, SingleHubModel, TrustModel}

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
    keyStoreType: KeyStoreType = KeyStoreType.Default,
    trustModel: TrustModel = SingleHubModel(false),
    remoteSiteDescriptors: Seq[RemoteSiteDescriptor] = Nil) // TODO: make this non-default once crypto is deleted
{
  override def toString = scala.runtime.ScalaRunTime._toString(this.copy(password = "REDACTED"))
}