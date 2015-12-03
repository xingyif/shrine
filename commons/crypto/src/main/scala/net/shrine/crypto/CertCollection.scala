package net.shrine.crypto

import java.security.cert.X509Certificate
import java.security.KeyStore
import net.shrine.protocol.CertId
import java.security.Principal

/**
 * @author clint
 * @date Nov 22, 2013
 */

//todo remove this trait. There is only KeyStoreCertCollection
trait CertCollection extends Iterable[X509Certificate] {
  def myCertId: Option[CertId]
  
  def myCert: Option[X509Certificate]
  
  def caCertAliases: Seq[String]
  
  def caCerts: Map[Principal, X509Certificate]
  
  def myKeyPair: KeyPair
  
  override def size: Int
  
  def get(id: CertId): Option[X509Certificate]
  
  override def iterator: Iterator[X509Certificate]
  
  def ids: Iterable[CertId]
  
  def caIds: Iterable[CertId]
}

object CertCollection {
  def getIssuer(cert: X509Certificate): Principal = cert.getIssuerX500Principal
}