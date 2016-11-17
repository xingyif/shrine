package net.shrine.crypto

import java.math.BigInteger
import java.security.{KeyPairGenerator, PrivateKey, SecureRandom}
import java.util.Date

import net.shrine.util.NonEmptySeq
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

/**
  * Created by ty on 11/10/16.
  */
object CertificateCreator {
  val bc = BouncyKeyStoreCollection
  // generate a key pair
  def createSelfSignedCertEntry(alias: String, cn: String, dc: String): KeyStoreEntry = {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA", bc.provider)
    keyPairGenerator.initialize(4096, new SecureRandom())
    val keyPair = keyPairGenerator.generateKeyPair()

    createSignedCertEntry(alias, cn, dc, keyPair.getPrivate, keyPair)
  }

  def createSignedCertEntry(alias: String, cn: String, dc: String, signingKey: PrivateKey, keyPair: java.security.KeyPair): KeyStoreEntry = {
    val name: X500Name = new X500Name(s"cn=$cn")
    val subject = new X500Name(s"dc=$dc")
    val serial = BigInteger.valueOf(System.currentTimeMillis())
    val notBefore = new Date(0)
    val notAfter = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 10)
    val subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(ASN1Sequence.getInstance(keyPair.getPublic.getEncoded))

    val certBuilder = new X509v3CertificateBuilder(name, serial, notBefore, notAfter, subject, subjectPublicKeyInfo)
    val certHolder = certBuilder.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(signingKey))
    val cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder)
    KeyStoreEntry(cert, NonEmptySeq(alias), Some(keyPair.getPrivate))
  }

  def createSignedCertFromEntry(alias: String, cn: String, dc: String, signingEntry: KeyStoreEntry): KeyStoreEntry = {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC")
    keyPairGenerator.initialize(4096, new SecureRandom())
    val keyPair = keyPairGenerator.generateKeyPair()

    createSignedCertEntry(alias, cn, dc, signingEntry.privateKey.get, keyPair)
  }
}
