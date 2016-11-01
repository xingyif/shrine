package net.shrine.crypto2

import java.security.cert.X509Certificate
import java.security._
import java.time.{Clock, Instant}
import java.util.Date

import net.shrine.crypto.UtilHasher
import net.shrine.util.NonEmptySeq
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.cms._
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

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
  private val provider = new BouncyCastleProvider()

  def verify(signedBytes: Array[Byte], originalMessage: Array[Byte]): Boolean = {
    import scala.collection.JavaConversions._                                             // Treat Java Iterable as Scala Iterable
    val signers: SignerInformationStore = new CMSSignedData(signedBytes).getSignerInfos
    val verifier = new JcaSimpleSignerInfoVerifierBuilder().setProvider(provider).build(cert)

    signers.headOption.exists(signerInfo => signerInfo.verify(verifier))                  // We don't attach multiple signers to a cert
  }


  /**
    * Provided that this is a PrivateKey Entry, sign the incoming bytes.
    * @return Returns None if this is not a PrivateKey Entry
    */
  def sign(bytesToSign: Array[Byte]): Option[Array[Byte]] = {
    privateKey.map(key => {
      val signature = Signature.getInstance("SHA256withRSA", provider)
      signature.initSign(key)
      signature.update(bytesToSign)

      val sigGen = new JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(key)
      val cms = new CMSSignedDataGenerator()

      cms.addSignerInfoGenerator(
        new SignerInfoGeneratorBuilder(new BcDigestCalculatorProvider())
          .build(sigGen, certificateHolder))
      cms.generate(new CMSProcessableByteArray(signature.sign), true).getEncoded     // true means to envelop the signature in the given data
    })
  }

  def wasSignedBy(entry: KeyStoreEntry): Boolean = Try(cert.verify(entry.publicKey)).isSuccess

  def isExpired(clock: Clock = Clock.systemDefaultZone()): Boolean = {
    certificateHolder.getNotAfter.before(Date.from(Instant.now(clock)))
  }
}