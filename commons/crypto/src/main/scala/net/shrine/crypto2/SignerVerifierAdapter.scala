package net.shrine.crypto2

import javax.xml.datatype.XMLGregorianCalendar

import net.shrine.crypto.{Signer, SigningCertStrategy, Verifier}
import net.shrine.protocol.{BroadcastMessage, Signature}
import net.shrine.util.{XmlDateHelper, XmlGcEnrichments}

import scala.concurrent.duration.Duration

/**
  * An adapter object so that the new crypto package can coexist with the
  * existing Signer and Verifier interfaces
  * @param keyStoreCollection The BouncyKeyStoreCollection that is signing
  *                           and verifying broadcast messages
  */
case class SignerVerifierAdapter(keyStoreCollection: BouncyKeyStoreCollection)
  extends BouncyKeyStoreCollection with Signer with Verifier
{
  override def signBytes(bytesToSign: Array[Byte]): Array[Byte] = keyStoreCollection.signBytes(bytesToSign)

  override def verifyBytes(cmsEncodedSignature: Array[Byte], originalMessage: Array[Byte]): Boolean = keyStoreCollection.verifyBytes(cmsEncodedSignature, originalMessage)

  override val myEntry: KeyStoreEntry = keyStoreCollection.myEntry

  override def allEntries: Iterable[KeyStoreEntry] = keyStoreCollection.allEntries

  override def sign(message: BroadcastMessage, signingCertStrategy: SigningCertStrategy): BroadcastMessage = {
    val certAdapter = CertCollectionAdapter(keyStoreCollection)
    val timeStamp = XmlDateHelper.now
    val dummyCertId = certAdapter.myCertId.get
    val signedBytes = signBytes(toBytes(message, timeStamp))
    val sig = Signature(timeStamp, dummyCertId, None, signedBytes)
    message.withSignature(sig)
  }

  override def verifySig(message: BroadcastMessage, maxSignatureAge: Duration): Boolean = {
    val logSigFailure = (b:Boolean) => {
      if (!b) {
        UnknownSignatureProblem(message)
        warn(s"Error verifying signature for message with id '${message.requestId}'")
      }
      b
    }

    message.signature.exists(sig =>
      {
        val notTooOl = notTooOld(sig, maxSignatureAge, message)
        val verify = verifyBytes(sig.value.array, toBytes(message, sig.timestamp))
        notTooOl && logSigFailure(verify)
      }
    )
  }

  override def remoteSites: Seq[RemoteSite] = keyStoreCollection.remoteSites

  // Has the signature expired?
  private def notTooOld(sig: Signature, maxSignatureAge: Duration, message: BroadcastMessage): Boolean = {
    import XmlGcEnrichments._

    val sigValidityEndTime: XMLGregorianCalendar = sig.timestamp + maxSignatureAge
    val now = XmlDateHelper.now
    val timeout = sigValidityEndTime > now

    if (!timeout) warn(s"Could not validate message with id '${message.requestId}' due to " +
      s"exceeding max timeout of $maxSignatureAge")

    timeout
  }

  // Concatenates with the timestamp. This is how it's converted to bytes in the
  // the DefaultSignerVerifier, but now that we're using CMS I don't think this is necessary
  // anymore. It was only done before to ensure unique signatures, I believe.
  private def toBytes(message: BroadcastMessage, timestamp: XMLGregorianCalendar): Array[Byte] = {
    val messageXml = message.copy(signature = None).toXmlString
    val timestampXml = timestamp.toXMLFormat

    (messageXml + timestampXml).getBytes("UTF-8")
  }
}