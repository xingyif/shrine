package net.shrine.crypto

import java.io.{IOException, FileInputStream, InputStream, File}
import java.security.{KeyStore, PrivateKey, Key, Principal}
import java.security.cert.X509Certificate
import javax.naming.ldap.{Rdn, LdapName}

import net.shrine.log.Loggable
import net.shrine.protocol.CertId

import scala.collection.JavaConverters.enumerationAsScalaIteratorConverter

/**
 * @author clint
 * @since Nov 22, 2013
 */
final case class KeyStoreCertCollection(keystore: KeyStore, descriptor: KeyStoreDescriptor) extends CertCollection with Loggable {
  override def size: Int = keystore.size
  
  override def get(id: CertId): Option[X509Certificate] = certsById.get(id)
  
  override def iterator: Iterator[X509Certificate] = certsById.valuesIterator ++ caCerts.valuesIterator

  override def ids: Iterable[CertId] = certsById.keys
  
  import KeyStoreCertCollection.toCertId
  
  override lazy val caIds: Iterable[CertId] = caCerts.values.map(toCertId)
  
  override def caCertAliases: Seq[String] = descriptor.caCertAliases
  
  override lazy val caCerts: Map[Principal, X509Certificate] = {
    caCertAliases.flatMap(getX509Cert).map(cert => CertCollection.getIssuer(cert) -> cert).toMap
  }

  override lazy val myCert: Option[X509Certificate] = descriptor.privateKeyAlias.flatMap(getX509Cert)
  
  override lazy val myCertId: Option[CertId] = myCert.map(toCertId)

  lazy val myCommonName:Option[String] = {
    myCert.flatMap{cert:X509Certificate =>
      KeyStoreCertCollection.extractCommonName(cert)
    }
  }

  override val myKeyPair: KeyPair = {
    val privateKeyAlias: String = descriptor.privateKeyAlias match {
      case Some(alias) =>
        if(isPrivateKey(alias)) { alias }
        else throw new Exception(s"No key, or no private key component, at alias '$alias'")

      case _ =>
        val privateKeyAliases = keystore.aliases.asScala.filter(isPrivateKey).toIndexedSeq

        privateKeyAliases.size match {
          case 1 =>
            val alias = privateKeyAliases.head
            info(s"Found one cert with a private key, with alias '$alias'")
            alias

          case 0 => throw new Exception(s"No aliases point to certs with private keys.  Known aliases are: $privateKeyAliases")
          case n => throw new Exception(s"$n aliases point to certs with private keys: $privateKeyAliases; specify the private key to use with the privateKeyAlias option")
        }
    }
    
    val keyPairOption = for {
      cert <- getX509Cert(privateKeyAlias)
      privateKey <- getPrivateKey(privateKeyAlias)
    } yield KeyPair(cert.getPublicKey, privateKey)
    
    require(keyPairOption.isDefined, "Private key alias must be defined, and identify a cert with a private key component, or exactly one cert with a private key component must be present in the keystore")
    
    keyPairOption.get
  }

  private def getKey(alias: String): Option[Key] = {
    Option(keystore.getKey(alias, descriptor.password.toCharArray))
  }
  
  private def isPrivateKey(alias: String): Boolean = {
    getKey(alias).exists(_.isInstanceOf[PrivateKey])
  }
  
  private def getPrivateKey(alias: String): Option[PrivateKey] = {
    getKey(alias).collect { case pk: PrivateKey => pk }
  }
  
  private lazy val certsById: Map[CertId, X509Certificate] = {
    import scala.collection.JavaConverters._
    
    val nonCaAliases = keystore.aliases.asScala.toSet -- caCertAliases
    
    val certs = nonCaAliases.toSeq.flatMap(getX509Cert)

    certs.map(cert => (toCertId(cert), cert)).toMap
  }
  
  private[crypto] def getX509Cert(alias: String): Option[X509Certificate] = {
    Option(keystore.getCertificate(alias).asInstanceOf[X509Certificate])
  }
}

object KeyStoreCertCollection extends Loggable {

  /** Try the file system if a keystore file exists, else try the classpath*/
  def fromFileRecoverWithClassPath(descriptor: KeyStoreDescriptor): KeyStoreCertCollection = {
    if(new File(descriptor.file).exists) fromFile(descriptor)
    else fromClassPathResource(descriptor)
  }

  def fromFile(descriptor: KeyStoreDescriptor): KeyStoreCertCollection = {
    require(new File(descriptor.file).exists,s"Keystore file '${descriptor.file}' exists? ${new File(descriptor.file).exists}")
    fromStream(descriptor, new FileInputStream(_))
  }
  
  def fromClassPathResource(descriptor: KeyStoreDescriptor): KeyStoreCertCollection = {
    fromStream(descriptor, getClass.getClassLoader.getResourceAsStream)
  }
  
  def fromStream(descriptor: KeyStoreDescriptor, streamFrom: String => InputStream): KeyStoreCertCollection = {
    KeyStoreCertCollection(fromStreamHelper(descriptor, streamFrom), descriptor)
  }

  def fromStreamHelper(descriptor: KeyStoreDescriptor, streamFrom: String => InputStream): KeyStore = {
    def toString(descriptor: KeyStoreDescriptor) = descriptor.copy(password = "********").toString

    debug(s"Loading keystore using descriptor: ${toString(descriptor)}")

    val stream = streamFrom(descriptor.file)

    require(stream != null,s"null stream for descriptor ${toString(descriptor)}¬")

    val keystore = KeyStore.getInstance(descriptor.keyStoreType.name)

    try {
      keystore.load(stream, descriptor.password.toCharArray)
    } catch {case x:IOException => throw new IOException(s"Unable to load keystore from $descriptor",x)}

    import scala.collection.JavaConverters._

    debug(s"Keystore aliases: ${keystore.aliases.asScala.mkString(",")}")

    debug(s"Keystore ${toString(descriptor)} loaded successfully")

    keystore
  }
  
  private[crypto] def toCertId(cert: X509Certificate): CertId = {
    //TODO: Is getSubjectDN right for a human-readable name?
    CertId(cert.getSerialNumber, Option(cert.getSubjectDN.getName))
  }

  def extractCommonName(cert:X509Certificate):Option[String] = {

    val ldapDn = new LdapName(cert.getSubjectX500Principal.getName)
    import collection.JavaConverters._
    val rdns: Array[Rdn] = ldapDn.getRdns.asScala.toArray
    rdns.collectFirst{case rdn:Rdn if rdn.getType == "CN" => rdn.getValue.toString}
  }
}