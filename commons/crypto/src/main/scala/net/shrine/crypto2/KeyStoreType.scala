package net.shrine.crypto2

/**
 * @author clint
 * @date Nov 22, 2013
 */
final case class KeyStoreType private (name: String) {
  override def toString: String = name
}

object KeyStoreType {
  def Default = PKCS12

  val PKCS12 = KeyStoreType("PKCS12")

  val JKS = KeyStoreType("JKS")

  def valueOf(name: String): Option[KeyStoreType] = {
    def normalize(s: String) = s.toLowerCase
    
    val n = normalize(name)

    if (n == normalize(JKS.name)) { Some(JKS) }
    else if (n == normalize(PKCS12.name)) { Some(PKCS12) }
    else { None }
  }
}