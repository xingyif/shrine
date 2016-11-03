package net.shrine.crypto2

import java.security.cert.X509Certificate
import java.security._
import java.time.{Clock, Instant}
import java.util
import java.util.Date

import net.shrine.crypto.UtilHasher
import net.shrine.util.NonEmptySeq
import org.bouncycastle.asn1.x500.style.{BCStyle, IETFUtils}
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.{JcaCertStore, JcaX509CertificateConverter, JcaX509CertificateHolder}
import org.bouncycastle.cms._
import org.bouncycastle.cms.jcajce.{JcaSignerInfoGeneratorBuilder, JcaSimpleSignerInfoVerifierBuilder}
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.{ContentSigner, ContentVerifier}
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import org.bouncycastle.operator.jcajce.{JcaContentSignerBuilder, JcaContentVerifierProviderBuilder, JcaDigestCalculatorProviderBuilder}
import org.bouncycastle.util.{Selector, Store}

import scala.util.Try

/**
  * Created by ty on 10/26/16.
  * Represents a single entry in a key store collection. As a key entry may be either a PrivateKey Entry
  * or a TrustedCert Entry, there's no guarantee that there is a privateKey available
  *
  * @param cert: The x509 certificate in the entry
  * @param aliases: The alias of the certificate in the keystore
  * @param privateKey: The private key of the certificate, which is only available if this keystore represents
  *                    a private key entry (i.e., do we own this certificate?)
  */
final case class KeyStoreEntry(cert: X509Certificate, aliases: NonEmptySeq[String], privateKey: Option[PrivateKey]) {
  val publicKey:PublicKey = cert.getPublicKey
  val certificateHolder = new JcaX509CertificateHolder(cert)                              // Helpful methods are defined in the cert holder.
  val isSelfSigned: Boolean = certificateHolder.getSubject == certificateHolder.getIssuer // May or may not be a CA
  val formattedSha256Hash: String = UtilHasher.encodeCert(cert, "SHA-256")

  val commonName: Option[String] = KeyStoreEntry.getCommonNameFromCert(certificateHolder)


//    certificateHolder.getSubject.getRDNs(BCStyle.CN).headOption.flatMap(rdn =>
//                                      Option(rdn.getFirst).map(cn => IETFUtils.valueToString(cn.getValue)))

  private val provider = new BouncyCastleProvider()

  def verify(signedBytes: Array[Byte], originalMessage: Array[Byte]): Boolean = {
    KeyStoreEntry.extractCertHolder(signedBytes).exists(_.isSignatureValid(
        new JcaContentVerifierProviderBuilder()
          .setProvider(provider).build(certificateHolder)
      )
    )
  }


  /**
    * Provided that this is a PrivateKey Entry, sign the incoming bytes.
    * @return Returns None if this is not a PrivateKey Entry
    */
  def sign(bytesToSign: Array[Byte]): Option[Array[Byte]] = {
    import scala.collection.JavaConversions._
    val SHA256 = "SHA256withRSA"
    privateKey.map(key => {
      val signature = Signature.getInstance(SHA256, provider)
      signature.initSign(key)
      signature.update(bytesToSign)

      val data = new CMSProcessableByteArray(signature.sign())
      val gen = new CMSSignedDataGenerator()

      gen.addCertificates(new JcaCertStore(Seq(cert)))
      val result = gen.generate(data, true).getEncoded
      result

    })
  }

  def wasSignedBy(entry: KeyStoreEntry): Boolean = wasSignedBy(entry.publicKey)

  def wasSignedBy(publicKey: PublicKey): Boolean = certificateHolder.isSignatureValid(
    new JcaContentVerifierProviderBuilder().setProvider(provider).build(publicKey)
  )

  def signed(cert: X509Certificate): Boolean = new JcaX509CertificateHolder(cert).isSignatureValid(
    new JcaContentVerifierProviderBuilder().setProvider(provider).build(publicKey)
  )


  def isExpired(clock: Clock = Clock.systemDefaultZone()): Boolean = {
    certificateHolder.getNotAfter.before(Date.from(Instant.now(clock)))
  }
}

object KeyStoreEntry {
  def extractCertHolder(signedBytes: Array[Byte]): Option[X509CertificateHolder] = {
    import scala.collection.JavaConversions._
    val signedData = new CMSSignedData(signedBytes)
    val store = signedData.getCertificates.asInstanceOf[Store[X509CertificateHolder]]
    val certCollection = store.getMatches(SelectAll)
    certCollection.headOption
  }

  def getCommonNameFromCert(certHolder: X509CertificateHolder): Option[String] = {
    for {
      rdn <- certHolder.getSubject.getRDNs(BCStyle.CN).headOption
      cn <- Option(rdn.getFirst)
    } yield IETFUtils.valueToString(cn.getValue)
  }

  def extractCommonName(signedBytes: Array[Byte]): Option[String] = {
    for {
      certHolder <- extractCertHolder(signedBytes)
      commonName <- getCommonNameFromCert(certHolder)
    } yield commonName
  }
}

object SelectAll extends Selector[X509CertificateHolder] {
  override def `match`(obj: X509CertificateHolder): Boolean = true
}