package net.shrine.crypto2

import java.security.{PrivateKey, PublicKey}
import java.security.cert.X509Certificate

import net.shrine.crypto.UtilHasher
import org.bouncycastle.asn1.x500.style.{BCStyle, IETFUtils}
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.cms._
import org.bouncycastle.operator.ContentVerifierProvider
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import org.bouncycastle.operator.jcajce.{JcaContentSignerBuilder, JcaContentVerifierProviderBuilder}
import org.bouncycastle.util.Store

import scala.collection.convert.{DecorateAsScala, WrapAsScala}

/**
  * Created by ty on 10/26/16.
  * Represents a single entry in a key store collection. As a key entry may be either a PrivateKey Entry
  * or a TrustedCert Entry, there's no guarantee that there is a privateKey available
  *
  * @param cert: The x509 certificate in the entry
  * @param alias: The alias of the certificate in the keystore
  * @param publicKey: The public key of the certifcate, which can be used to verify incoming messages
  * @param privateKey: The private key of the certificate, which is only available if this keystore represents
  *                    a private key entry (i.e., do we own this certificate?)
  */
case class KeyStoreEntry(cert: X509Certificate, alias: String, publicKey: PublicKey, privateKey: Option[PrivateKey]) {
  val certificateHolder = new JcaX509CertificateHolder(cert)                              // Helpful methods are defined in the cert holder.
  val isSelfSigned: Boolean = certificateHolder.getSubject == certificateHolder.getIssuer // May or may not be a CA
  val formattedSha256Hash: String = UtilHasher.encodeCert(cert, "SHA-256")

  def verify(signedBytes: Array[Byte], signatureBytes: Array[Byte]): Boolean = {
    import scala.collection.JavaConversions._                                             // Treat Java Iterable as Scala Iterable
    val signers: SignerInformationStore = new CMSSignedData(signedBytes).getSignerInfos

    signers.headOption.exists(signerInfo => signerInfo.verify(new JcaSimpleSignerInfoVerifierBuilder().build(cert)))
  }


  /**
    * Provided that this is a PrivateKey Entry, sign the incoming bytes.
    * @return Returns None if this is not a PrivateKey Entry
    */
  def sign(bytesToSign: Array[Byte]): Option[Array[Byte]] = {
    privateKey.map(key => {
      val sigGen = new JcaContentSignerBuilder("SHA256withRSA").build(key)
      val cms = new CMSSignedDataGenerator()
      cms.addSignerInfoGenerator(
        new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider())
          .build(sigGen, certificateHolder))
      cms.generate(new CMSProcessableByteArray(bytesToSign), true).getEncoded     // true means to envelop the signature in the given data
    })
  }

}

object KeyStoreEntry {
  def apply(cert: X509Certificate): KeyStoreEntry = {
    val jca = new JcaX509CertificateHolder(cert)
    val subject = jca.getSubject
    val issuer = jca.getIssuer
    val cn = subject.getRDNs(BCStyle.CN)(0)
    IETFUtils.valueToString(cn.getFirst.getValue())
    val sha256 = UtilHasher.encodeCert(cert, "SHA-256")
    val isSelfSigned = subject.equals(issuer)
    ???
  }
}
