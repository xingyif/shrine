package net.shrine.crypto2

import java.security.Principal
import java.security.cert.X509Certificate

import net.shrine.crypto.{CertCollection, KeyPair}
import net.shrine.protocol.CertId

/**
  * Created by ty on 10/25/16.
  */
class DownstreamKeyStoreCollection extends CertCollection {



  override def myCertId: Option[CertId] = ???

  override def myCert: Option[X509Certificate] = ???

  override def caCertAliases: Seq[String] = ???

  override def caCerts: Map[Principal, X509Certificate] = ???

  override def myKeyPair: KeyPair = ???

  override def get(id: CertId): Option[X509Certificate] = ???

  override def iterator: Iterator[X509Certificate] = ???

  override def ids: Iterable[CertId] = ???

  override def caIds: Iterable[CertId] = ???
}
