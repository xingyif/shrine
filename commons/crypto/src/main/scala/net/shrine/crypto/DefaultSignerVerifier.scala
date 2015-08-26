package net.shrine.crypto

import java.security.PrivateKey
import java.security.{ Signature => JSig }
import java.security.cert.Certificate
import javax.xml.datatype.XMLGregorianCalendar
import net.shrine.log.Loggable
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.CertId
import net.shrine.protocol.Signature
import net.shrine.util.{Tries, XmlDateHelper, XmlGcEnrichments}
import scala.concurrent.duration.Duration
import net.shrine.protocol.CertData
import scala.util.Success
import scala.util.Failure
import java.security.cert.X509Certificate
import scala.util.Try
import java.security.PublicKey

/**
 * @author clint
 * @since Nov 25, 2013
 */
final class DefaultSignerVerifier(certCollection: CertCollection) extends Signer with Verifier with Loggable {
  logStartup()

  import DefaultSignerVerifier._

  import SigningCertStrategy._

  override def sign(message: BroadcastMessage, signingCertStrategy: SigningCertStrategy): BroadcastMessage = {
    val signatureOption = for {
      signedBy <- certCollection.myCertId
      KeyPair(_, myPrivateKey) = certCollection.myKeyPair
      myCert <- certCollection.myCert
    } yield {
      val timestamp = XmlDateHelper.now

      //TODO: Find way to attach only the public part of the signing cert, plus its signatures
      //TODO: Currently the whole signing cert is attached (right?)
      val signingCert = {
        if (signingCertStrategy.attachSigningCert) { Some(CertData(myCert)) }
        else { None }
      }

      Signature(timestamp, signedBy, signingCert, DefaultSignerVerifier.sign(myPrivateKey, toBytes(message, timestamp)))
    }

    val sig = signatureOption.getOrElse(throw new Exception(s"Can't sign, no private keys.  Known ids: ${certCollection.ids}"))

    message.withSignature(sig)
  }

  import Tries.toTry

  private[crypto] def isSignedByTrustedCA(attachedCert: X509Certificate): Try[Boolean] = {
    for {
      attachedCertSignerCert <- toTry(certCollection.caCerts.get(CertCollection.getIssuer(attachedCert)))(new Exception(s"Couldn't find CA certificate with issuer DN '${attachedCert.getIssuerDN}'; known CA cert aliases: ${certCollection.caCertAliases.mkString(",")}"))
      caPublicKey = attachedCertSignerCert.getPublicKey
    } yield {
      attachedCert.isSignedBy(caPublicKey)
    }
  }

  private[crypto] def obtainAndValidateSigningCert(signature: Signature): Try[X509Certificate] = {
    signature.signingCert match {
      //If the signing cert was sent with the signature, and the signing cert was signed by a CA we trust 
      case Some(signingCertData) => {
        for {
          signingCert <- Try(signingCertData.toCertificate)
          signedByTrustedCA <- isSignedByTrustedCA(signingCert)
        } yield {
          if (signedByTrustedCA) { signingCert }
          else {
            throw new Exception(s"Couldn't verify: signing cert with serial '${signingCert.getSerialNumber}' was part of the signature, but was not signed by any CA we trust.  Aliases of trusted CAs are: ${certCollection.caCertAliases.map(s => s"'$s'").mkString(",")}")
          }
        }
      }
      //Otherwise, look up the signing cert in our keystore by its CertId 
      case None => {
        val signerCertOption = certCollection.get(signature.signedBy)

        toTry(signerCertOption)(new Exception(s"Couldn't verify: can't find signer key with CertId ${signature.signedBy}"))
      }
    }
  }

  override def verifySig(message: BroadcastMessage, maxSignatureAge: Duration): Boolean = {
    def notTooOld(sig: Signature): Boolean = {
      import scala.concurrent.duration._
      import XmlGcEnrichments._

      val sigValidityEndTime = sig.timestamp + maxSignatureAge

      sigValidityEndTime > XmlDateHelper.now
    }

    message.signature match {
      case None => false
      case Some(signature) => {
        val signerCertAttempt: Try[X509Certificate] = obtainAndValidateSigningCert(signature)

        val verificationAttempt = for {
          signerCert <- signerCertAttempt
          if notTooOld(signature)
        } yield {
          DefaultSignerVerifier.verify(signerCert, toBytes(message, signature.timestamp), signature.value.array)
        }

        verificationAttempt match {
          case Success(result) => result
          case Failure(reason) => {
            warn(s"Error verifying signature for message with id '${message.requestId}': ", reason)

            false
          }
        }
      }
    }
  }

  private def logStartup() {
    debug(s"DefaultSignerVerifier using cert collection: ")

    debug(s"Private key id: ${certCollection.myCertId}")

    debug(s"Known certs: ")

    def certIdToMessage(certId: CertId): String = s"  ${certId.serial} with name '${certId.name.getOrElse("")}'" 
    
    certCollection.ids.foreach { certId =>
      debug(certIdToMessage(certId))
    }
    
    debug(s"Known CA certs: ")

    certCollection.caCerts.values.map(KeyStoreCertCollection.toCertId).foreach { certId =>
      debug(certIdToMessage(certId))
    }
  }
}

object DefaultSignerVerifier {
  val signatureAlgorithm = "SHA256withRSA"

  private implicit final class HasBooleanSignedBy(val cert: X509Certificate) extends AnyVal {
    def isSignedBy(caPubKey: PublicKey): Boolean = Try { cert.verify(caPubKey); true }.getOrElse(false)
  }

  private def toBytes(message: BroadcastMessage, timestamp: XMLGregorianCalendar): Array[Byte] = {
    val messageXml = message.copy(signature = None).toXmlString

    val timestampXml = timestamp.toXMLFormat

    (messageXml + timestampXml).getBytes("UTF-8")
  }

  private[crypto] def sign(signingKey: PrivateKey, bytes: Array[Byte]): Array[Byte] = {
    val signerVerifier = getSignerVerifier
    
    signerVerifier.initSign(signingKey)

    signerVerifier.update(bytes)

    signerVerifier.sign
  }

  private[crypto] def verify(signerCert: Certificate, signedBytes: Array[Byte], signatureBytes: Array[Byte]): Boolean = {
    val signerVerifier = getSignerVerifier

    signerVerifier.initVerify(signerCert)

    signerVerifier.update(signedBytes)

    signerVerifier.verify(signatureBytes)
  }

  private def getSignerVerifier: JSig = JSig.getInstance(signatureAlgorithm)
}