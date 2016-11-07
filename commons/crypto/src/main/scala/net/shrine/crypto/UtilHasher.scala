package net.shrine.crypto

import java.security.MessageDigest
import java.security.cert.X509Certificate

import net.shrine.crypto2.BouncyKeyStoreCollection
import net.shrine.spray.{BadShaResponse, FoundShaResponse, NotFoundShaResponse, ShaResponse}
import org.bouncycastle.asn1.x500.style.{BCStyle, IETFUtils}
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder


/**
  * Created by ty on 10/18/16.
  */
case class UtilHasher(certCollection: BouncyKeyStoreCollection) {

  def containsCertWithSig(sha256: String):Option[X509Certificate] = {
    val typedNone:Option[X509Certificate] = None
    certCollection.allEntries.map(_.cert).foldLeft(typedNone)((acc, cur) => matchSig(acc, cur, sha256))
  }

  private def matchSig(maybeCert: Option[X509Certificate], cert:X509Certificate, sha256:String): Option[X509Certificate] = {
    if (maybeCert.isDefined)
      maybeCert
    else if (UtilHasher.encodeCert(cert, "SHA-256").toLowerCase == sha256.toLowerCase)
      Some(cert)
    else
      None
  }

  def validSignatureFormat(sha256: String): Boolean = {
    sha256.matches("([A-F,0-9]{2}:){31}[A-F,0-9]{2}")
  }

  def handleSig(sha256:String): ShaResponse = {
    val mySha =  UtilHasher.encodeCert(certCollection.myEntry.cert, "SHA-256")
    if (validSignatureFormat(sha256)) {
      containsCertWithSig(sha256).fold(NotFoundShaResponse(mySha): ShaResponse)(_ => FoundShaResponse(mySha))
    } else {
      BadShaResponse
    }
  }
}

object UtilHasher {

  def encodeCert(cert: X509Certificate, hasher: String): String = {
    val encoding = MessageDigest.getInstance(hasher)
    def toHex(buf: Array[Byte]): String = buf.map("%02X".format(_)).mkString(":")

    toHex(encoding.digest(cert.getEncoded))
  }
}
