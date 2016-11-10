package net.shrine.crypto

import java.security.Signature

import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.{CMSProcessableByteArray, CMSSignedData, CMSSignedDataGenerator}

import scala.util.Try

/**
  * Created by ty on 11/7/16.
  */
abstract class AbstractHubCertCollection(override val myEntry: KeyStoreEntry, caEntry: KeyStoreEntry)
  extends BouncyKeyStoreCollection {

  override val allEntries: Iterable[KeyStoreEntry] = myEntry +: caEntry +: Nil

  /**
    *
    * First, extract the X509Certificate from the signature.
    * Second, verify that the certificate was signed by our CA
    * Third, ensure that the attached X509Certificate actually signed the incoming
    * signature and data
    */
  override def verifyBytes(cmsEncodedSignature: Array[Byte], originalMessage: Array[Byte]) = {
    val sigTry = Try(new CMSSignedData(cmsEncodedSignature))
    if (sigTry.isFailure)
      CryptoErrors.invalidSiganatureFormat(cmsEncodedSignature)
    else {
      val sig = sigTry.get.getSignedContent.getContent.asInstanceOf[Array[Byte]]

      KeyStoreEntry.extractCertHolder(cmsEncodedSignature).exists(incomingCert => {
        val signedByCa = KeyStoreEntry.certSignedOtherCert(caEntry.certificateHolder, incomingCert)
        val x509Cert = KeyStoreEntry.extractX509Cert(incomingCert)
        val signer = Signature.getInstance(
          BouncyKeyStoreCollection.SHA256,
          provider)

        signer.initVerify(x509Cert.getPublicKey)
        signer.update(originalMessage)
        val correctSignature = signer.verify(sig)

        signedByCa && correctSignature
      })
    }
  }

  /**
    * First, sign the incoming bytes using the private key of our PrivateKeyEntry
    * Then, encode the signature into CMSSignedData, and encode our X509Certificate into the signature
    *
    * Note, it's perfectly safe to encode our X509Certificate (it's basically our public key)
    */
  override def signBytes(bytesToSign: Array[Byte]): Array[Byte] = {
    import scala.collection.JavaConversions._
    val data = new CMSProcessableByteArray(myEntry.sign(bytesToSign).getOrElse(CryptoErrors.noKeyError(myEntry)))
    val gen = new CMSSignedDataGenerator()

    gen.addCertificates(new JcaCertStore(Seq(myEntry.certificateHolder)))
    gen.generate(data, true).getEncoded
  }
}


/**
  * Created by ty on 10/25/16.
  */
case class DownStreamCertCollection(override val myEntry: KeyStoreEntry, caEntry: KeyStoreEntry, hubSite: RemoteSite)
  extends AbstractHubCertCollection(myEntry, caEntry)
{
  override val remoteSites = hubSite +: Nil
}


/**
  * Created by ty on 11/4/16.
  */
case class HubCertCollection(override val myEntry: KeyStoreEntry, caEntry: KeyStoreEntry, override val remoteSites: Seq[RemoteSite])
  extends AbstractHubCertCollection(myEntry, caEntry)
