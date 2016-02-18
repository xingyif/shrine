package net.shrine.crypto

import net.shrine.util.{Base64, ShouldMatchersForJUnit, XmlGcEnrichments}
import org.junit.Test
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.protocol.query.Modifiers
import net.shrine.protocol.query.Or
import net.shrine.protocol.ReadQueryResultRequest
import net.shrine.protocol.DefaultBreakdownResultOutputTypes
import net.shrine.protocol.query.Constrained
import net.shrine.protocol.query.ValueConstraint
import net.shrine.protocol.CertId
import java.math.BigInteger
import java.security.cert.X509Certificate
import java.security.cert.CertificateFactory
import scala.io.Source
import java.io.ByteArrayInputStream

/**
 * @author clint
 * @since Nov 27, 2013
 */
final class DefaultSignerVerifierTest extends ShouldMatchersForJUnit {
  private val authn = AuthenticationInfo("some-domain", "some-username", Credential("sadkljlajdl", isToken = false))

  private val certCollection = TestKeystore.certCollection

  private val signerVerifier = new DefaultSignerVerifier(certCollection)

  import SigningCertStrategy._
  import scala.concurrent.duration._

  @Test
  def testIssuersMatchBetweenCertsWithIPsInDistinguishedNames(): Unit = {
    def readCert(fileName: String): X509Certificate = {
      val factory = CertificateFactory.getInstance("X.509")
      
      val source = Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(fileName))
      
      val encodedCertData = try { source.mkString } finally { source.close() }
      
      val byteStream = new ByteArrayInputStream(Base64.fromBase64(encodedCertData))
      
      try { factory.generateCertificate(byteStream).asInstanceOf[X509Certificate] }
      finally { byteStream.close() }
    }
    
    val ca = readCert("test-caroot.pem")
    val alpha = readCert("test-alpha-signed.pem")
    val beta = readCert("test-beta-signed.pem")
    val gamma = readCert("test-gamma-signed.pem")
    
    def shouldMatch[F](field: X509Certificate => F)(a: X509Certificate, b: X509Certificate) {
      field(a) should equal(field(b))
      //Use options to handle null fields
      Option(field(a)).map(_.hashCode) should equal(Option(field(b)).map(_.hashCode))
    }
    
    shouldMatch(_.getIssuerDN)(ca, alpha)
    shouldMatch(_.getIssuerDN)(ca, beta)
    shouldMatch(_.getIssuerDN)(ca, gamma)
    
    shouldMatch(_.getIssuerX500Principal)(ca, alpha)
    shouldMatch(_.getIssuerX500Principal)(ca, beta)
    shouldMatch(_.getIssuerX500Principal)(ca, gamma)
    
    shouldMatch(_.getIssuerUniqueID)(ca, alpha)
    shouldMatch(_.getIssuerUniqueID)(ca, beta)
    shouldMatch(_.getIssuerUniqueID)(ca, gamma)
    
    ca.getSerialNumber should not equal(alpha.getSerialNumber)
    ca.getSerialNumber should not equal(beta.getSerialNumber)
    ca.getSerialNumber should not equal(gamma.getSerialNumber)
  }
  
  @Test
  def testSigningAndVerificationQueryDefWithSubQueries(): Unit = {
    //A failing case reported by Ben C.
    val queryDef = QueryDefinition.fromI2b2 {
      <query_definition>
        <query_name>(t) (493.90) Asthma(250.00) Diabet@15:32:58</query_name>
        <query_timing>ANY</query_timing>
        <specificity_scale>0</specificity_scale>
        <subquery_constraint>
          <first_query>
            <query_id>Event 1</query_id>
            <join_column>STARTDATE</join_column>
            <aggregate_operator>FIRST</aggregate_operator>
          </first_query>
          <operator>LESS</operator>
          <second_query>
            <query_id>Event 2</query_id>
            <join_column>STARTDATE</join_column>
            <aggregate_operator>FIRST</aggregate_operator>
          </second_query>
          <span>
            <operator>GREATEREQUAL</operator>
            <span_value>365</span_value>
            <units>DAY</units>
          </span>
        </subquery_constraint>
        <subquery>
          <query_id>Event 1</query_id>
          <query_type>EVENT</query_type>
          <query_name>Event 1</query_name>
          <query_timing>SAMEINSTANCENUM</query_timing>
          <specificity_scale>0</specificity_scale>
          <panel>
            <panel_number>1</panel_number>
            <panel_accuracy_scale>100</panel_accuracy_scale>
            <invert>0</invert>
            <panel_timing>SAMEINSTANCENUM</panel_timing>
            <total_item_occurrences>1</total_item_occurrences>
            <item>
              <hlevel>6</hlevel>
              <item_name>(493.90) Asthma, unspecified type, unspecified</item_name>
              <item_key>\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system (460-519.99)\Chronic obstructive pulmonary disease and allied conditions (490-496.99)\Asthma (493)\Asthma, unspecified (493.9)\(493.90) Asthma, unspecified type, unspecified\</item_key>
              <tooltip>Diagnoses\Diseases of the respiratory system (460-519.99)\Chronic obstructive pulmonary disease and allied conditions (490-496.99)\Asthma (493)\Asthma, unspecified (493.9)\(493.90) Asthma, unspecified type, unspecified\</tooltip>
              <class>ENC</class>
              <item_icon>LA</item_icon>
              <item_is_synonym>false</item_is_synonym>
            </item>
            <item>
              <hlevel>6</hlevel>
              <item_name>(493.91) Asthma, unspecified type, with status asthmaticus</item_name>
              <item_key>\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system (460-519.99)\Chronic obstructive pulmonary disease and allied conditions (490-496.99)\Asthma (493)\Asthma, unspecified (493.9)\(493.91) Asthma, unspecified type, with status asthmaticus\</item_key>
              <tooltip>Diagnoses\Diseases of the respiratory system (460-519.99)\Chronic obstructive pulmonary disease and allied conditions (490-496.99)\Asthma (493)\Asthma, unspecified (493.9)\(493.91) Asthma, unspecified type, with status asthmaticus\</tooltip>
              <class>ENC</class>
              <item_icon>LA</item_icon>
              <item_is_synonym>false</item_is_synonym>
            </item>
            <item>
              <hlevel>6</hlevel>
              <item_name>(493.92) Asthma, unspecified type, with (acute) exacerbation</item_name>
              <item_key>\\SHRINE\SHRINE\Diagnoses\Diseases of the respiratory system (460-519.99)\Chronic obstructive pulmonary disease and allied conditions (490-496.99)\Asthma (493)\Asthma, unspecified (493.9)\(493.92) Asthma, unspecified type, with (acute) exacerbation\</item_key>
              <tooltip>Diagnoses\Diseases of the respiratory system (460-519.99)\Chronic obstructive pulmonary disease and allied conditions (490-496.99)\Asthma (493)\Asthma, unspecified (493.9)\(493.92) Asthma, unspecified type, with (acute) exacerbation\</tooltip>
              <class>ENC</class>
              <item_icon>LA</item_icon>
              <item_is_synonym>false</item_is_synonym>
            </item>
          </panel>
        </subquery>
        <subquery>
          <query_id>Event 2</query_id>
          <query_type>EVENT</query_type>
          <query_name>Event 2</query_name>
          <query_timing>SAMEINSTANCENUM</query_timing>
          <specificity_scale>0</specificity_scale>
          <panel>
            <panel_number>1</panel_number>
            <panel_accuracy_scale>100</panel_accuracy_scale>
            <invert>0</invert>
            <panel_timing>SAMEINSTANCENUM</panel_timing>
            <total_item_occurrences>1</total_item_occurrences>
            <item>
              <hlevel>6</hlevel>
              <item_name>(250.00) Diabetes mellitus without mention of complication, type II or unspecified type, not stated as uncontrolled</item_name>
              <item_key>\\SHRINE\SHRINE\Diagnoses\Endocrine, nutritional and metabolic diseases, and immunity disorders (240-279.99)\Diseases of other endocrine glands (249-259.99)\Diabetes mellitus (250)\Diabetes mellitus without mention of complication (250.0)\(250.00) Diabetes mellitus without mention of complication, type II or unspecified type, not stated as uncontrolled\</item_key>
              <tooltip>Diagnoses\Endocrine, nutritional and metabolic diseases, and immunity disorders (240-279.99)\Diseases of other endocrine glands (249-259.99)\Diabetes mellitus (250)\Diabetes mellitus without mention of complication (250.0)\(250.00) Diabetes mellitus without mention of complication, type II or unspecified type, not stated as uncontrolled\</tooltip>
              <class>ENC</class>
              <item_icon>LA</item_icon>
              <item_is_synonym>false</item_is_synonym>
            </item>
            <item>
              <hlevel>6</hlevel>
              <item_name>(530.81) Esophageal reflux</item_name>
              <item_key>\\SHRINE\SHRINE\Diagnoses\Diseases of the digestive system (520-579.99)\Diseases of esophagus, stomach, and duodenum (530-539.99)\Diseases of esophagus (530)\Other specified disorders of esophagus (530.8)\(530.81) Esophageal reflux\</item_key>
              <tooltip>Diagnoses\Diseases of the digestive system (520-579.99)\Diseases of esophagus, stomach, and duodenum (530-539.99)\Diseases of esophagus (530)\Other specified disorders of esophagus (530.8)\(530.81) Esophageal reflux\</tooltip>
              <class>ENC</class>
              <item_icon>LA</item_icon>
              <item_is_synonym>false</item_is_synonym>
            </item>
          </panel>
        </subquery>
      </query_definition>
    }.get
    
    def shouldVerify(signingCertStrategy: SigningCertStrategy): Unit = {
      val resultTypes = DefaultBreakdownResultOutputTypes.toSet + ResultOutputType.PATIENT_COUNT_XML

      val unsignedMessage = BroadcastMessage(authn, RunQueryRequest("some-project-id", 12345.milliseconds, authn, Some("topic-id"), Some("Topic Name"), resultTypes, queryDef))

      val signedMessage = signerVerifier.sign(unsignedMessage, signingCertStrategy)

      signerVerifier.verifySig(signedMessage, 1.hour) should be(right = true)

      //NB: Simulate going from one machine to another
      val roundTripped = xmlRoundTrip(signedMessage)

      roundTripped.signature should equal(signedMessage.signature)

      signerVerifier.verifySig(roundTripped, 1.hour) should be(right = true)
    }

    //shouldVerify(Attach)
    shouldVerify(DontAttach)
  }

  //See https://open.med.harvard.edu/jira/browse/SHRINE-859
  @Test
  def testSigningAndVerificationQueryNameWithSpaces(): Unit = {
    def shouldVerify(queryName: String, signingCertStrategy: SigningCertStrategy): Unit = {
      val queryDef = QueryDefinition(queryName, Term("""\\PCORNET\PCORI\DEMOGRAPHIC\Age\&gt;= 65 years old\65\"""))

      val resultTypes = DefaultBreakdownResultOutputTypes.toSet + ResultOutputType.PATIENT_COUNT_XML

      val unsignedMessage = BroadcastMessage(authn, RunQueryRequest("some-project-id", 12345.milliseconds, authn, Some("topic-id"), Some("Topic Name"), resultTypes, queryDef))

      val signedMessage = signerVerifier.sign(unsignedMessage, signingCertStrategy)

      signerVerifier.verifySig(signedMessage, 1.hour) should be(right = true)

      //NB: Simulate going from one machine to another
      val roundTripped = xmlRoundTrip(signedMessage)

      roundTripped.signature should equal(signedMessage.signature)

      signerVerifier.verifySig(roundTripped, 1.hour) should be(right = true)
    }

    shouldVerify("foo", Attach)
    shouldVerify(" foo", Attach)
    shouldVerify("foo ", Attach)
    shouldVerify("fo o", Attach)
    shouldVerify(" 65 years old@13:23:00", Attach)

    shouldVerify("foo", DontAttach)
    shouldVerify(" foo", DontAttach)
    shouldVerify("foo ", DontAttach)
    shouldVerify("fo o", DontAttach)
    shouldVerify(" 65 years old@13:23:00", DontAttach)
  }

  @Test
  def testSigningAndVerification(): Unit = doTestSigningAndVerification(signerVerifier)

  @Test
  def testSigningAndVerificationAttachedKnownCertNotSignedByCA(): Unit = {
    //Messages will be signed with a key that's in our keystore, but is not signed by a CA 

    val descriptor = KeyStoreDescriptor(
      "shrine.keystore.multiple-private-keys",
      "chiptesting",
      Some("private-key-2"),
      Seq("carra ca"),
      KeyStoreType.JKS)

    val mySignerVerifier = new DefaultSignerVerifier(KeyStoreCertCollection.fromClassPathResource(descriptor))

    val unsignedMessage = BroadcastMessage(authn, DeleteQueryRequest("some-project-id", 12345.milliseconds, authn, 87356L))

    val signedMessage = mySignerVerifier.sign(unsignedMessage, Attach)

    mySignerVerifier.verifySig(signedMessage, 1.hour) should be(right = false)
  }

  @Test
  def testSigningAndVerificationAttachedUnknownCertNotSignedByCA(): Unit = {
    //Messages will be signed with a key that's NOT in our keystore, but is not signed by a CA 

    val signerDescriptor = KeyStoreDescriptor(
      "shrine.keystore.multiple-private-keys",
      "chiptesting",
      Some("private-key-2"), //This cert is NOT in TestKeystore.certCollection
      Seq("carra ca"),
      KeyStoreType.JKS)

    val signer: Signer = new DefaultSignerVerifier(KeyStoreCertCollection.fromClassPathResource(signerDescriptor))

    val verifier: Verifier = new DefaultSignerVerifier(TestKeystore.certCollection)

    val unsignedMessage = BroadcastMessage(authn, DeleteQueryRequest("some-project-id", 12345.milliseconds, authn, 87356L))

    val signedMessage = signer.sign(unsignedMessage, Attach)

    verifier.verifySig(signedMessage, 1.hour) should be(right = false)
  }

  private def doTestSigningAndVerification(signerVerifier: Signer with Verifier): Unit = {

    def doTest(signingCertStrategy: SigningCertStrategy) {

      val unsignedMessage = BroadcastMessage(authn, DeleteQueryRequest("some-project-id", 12345.milliseconds, authn, 87356L))

      val signedMessage = signerVerifier.sign(unsignedMessage, signingCertStrategy)

      (unsignedMessage eq signedMessage) should be(right = false)
      unsignedMessage should not equal (signedMessage)

      signedMessage.networkAuthn should equal(unsignedMessage.networkAuthn)
      signedMessage.request should equal(unsignedMessage.request)
      signedMessage.requestId should equal(unsignedMessage.requestId)

      unsignedMessage.signature should be(None)
      signedMessage.signature.isDefined should be(right = true)

      val sig = signedMessage.signature.get

      sig.timestamp should not be (null)
      sig.signedBy should equal(certCollection.myCertId.get)
      sig.value should not be (null)

      //The signed message should verify
      signerVerifier.verifySig(signedMessage, 1.hour) should be(right = true)

      def shouldNotVerify(message: BroadcastMessage) {
        signerVerifier.verifySig(xmlRoundTrip(message), 1.hour) should be(right = false)
      }

      //The unsigned one should not
      shouldNotVerify(unsignedMessage)

      //Expired sigs shouldn't verify
      signerVerifier.verifySig(signedMessage, 0.hours) should be(right = false)

      //modifying anything should prevent verification

      shouldNotVerify {
        val anotherRequest = signedMessage.request.asInstanceOf[DeleteQueryRequest].copy(networkQueryId = 123L)

        signedMessage.withRequest(anotherRequest)
      }

      shouldNotVerify {
        signedMessage.withRequestId(99999L)
      }

      shouldNotVerify {
        signedMessage.copy(networkAuthn = signedMessage.networkAuthn.copy(domain = "askldjlakjsd"))
      }

      shouldNotVerify {
        signedMessage.copy(networkAuthn = signedMessage.networkAuthn.copy(username = "askldjlakjsd"))
      }

      shouldNotVerify {
        signedMessage.copy(networkAuthn = signedMessage.networkAuthn.copy(credential = signedMessage.networkAuthn.credential.copy(isToken = true)))
      }

      shouldNotVerify {
        signedMessage.copy(networkAuthn = signedMessage.networkAuthn.copy(credential = signedMessage.networkAuthn.credential.copy(value = "oieutorutoirutioerutoireuto")))
      }

      shouldNotVerify {
        val timestamp = signedMessage.signature.get.timestamp

        import scala.concurrent.duration._
        import XmlGcEnrichments._

        val newTimestamp = timestamp + 123.minutes

        signedMessage.withSignature(signedMessage.signature.get.copy(timestamp = newTimestamp))
      }

      shouldNotVerify {
        val timestamp = signedMessage.signature.get.timestamp

        import scala.concurrent.duration._
        import XmlGcEnrichments._

        val newTimestamp = timestamp + (-99).minutes

        signedMessage.withSignature(signedMessage.signature.get.copy(timestamp = newTimestamp))
      }
    }

    doTest(Attach)
    doTest(DontAttach)
  }

  @Test
  def testSigningAndVerificationModifiedTerm(): Unit = {
    import scala.concurrent.duration._

    def doVerificationTest(signingCertStrategy: SigningCertStrategy, queryDef: QueryDefinition): Unit = {
      val req = RunQueryRequest("some-project-id", 12345.milliseconds, authn, Some("topic-id"), Some("Topic Name"), Set(ResultOutputType.PATIENT_COUNT_XML), queryDef)

      val unsignedMessage = BroadcastMessage(authn, req)

      val signedMessage = signerVerifier.sign(unsignedMessage, signingCertStrategy)

      (unsignedMessage eq signedMessage) should be(right = false)
      unsignedMessage should not equal (signedMessage)

      signedMessage.networkAuthn should equal(unsignedMessage.networkAuthn)
      signedMessage.request should equal(unsignedMessage.request)
      signedMessage.requestId should equal(unsignedMessage.requestId)

      unsignedMessage.signature should be(None)
      signedMessage.signature.isDefined should be(right = true)

      val sig = signedMessage.signature.get

      sig.timestamp should not be (null)
      sig.signedBy should equal(certCollection.myCertId.get)
      sig.value should not be (null)

      //The signed message should verify
      signerVerifier.verifySig(xmlRoundTrip(signedMessage), 1.hour) should be(right = true)
    }

    val t1 = Term("t1")
    val t2 = Term("t2")
    val t3 = Term("t3")

    doVerificationTest(Attach, QueryDefinition("foo", Term("")))

    doVerificationTest(Attach, QueryDefinition("foo", Constrained(t1, Some(Modifiers("n", "ap", t2.value)), None)))

    doVerificationTest(Attach, QueryDefinition("foo", Or(t1, Constrained(t2, Some(Modifiers("n", "ap", t3.value)), None))))

    doVerificationTest(Attach, QueryDefinition("foo", Or(t1, Constrained(t2, None, Some(ValueConstraint("foo", Some("bar"), "baz", "Nuh"))))))

    doVerificationTest(Attach, QueryDefinition("foo", Or(t1, Constrained(t2, None, Some(ValueConstraint("foo", None, "baz", "Nuh"))))))

    doVerificationTest(DontAttach, QueryDefinition("foo", Term("")))

    doVerificationTest(DontAttach, QueryDefinition("foo", Constrained(t1, Some(Modifiers("n", "ap", t2.value)), None)))

    doVerificationTest(DontAttach, QueryDefinition("foo", Or(t1, Constrained(t2, Some(Modifiers("n", "ap", t3.value)), None))))

    doVerificationTest(DontAttach, QueryDefinition("foo", Or(t1, Constrained(t2, None, Some(ValueConstraint("foo", Some("bar"), "baz", "Nuh"))))))

    doVerificationTest(DontAttach, QueryDefinition("foo", Or(t1, Constrained(t2, None, Some(ValueConstraint("foo", None, "baz", "Nuh"))))))
  }

  @Test
  def testSigningAndVerificationReadQueryResultRequest(): Unit = {
    def doTest(signingCertStrategy: SigningCertStrategy) {
      val localAuthn = AuthenticationInfo("i2b2demo", "shrine", Credential("SessionKey:PX4LlvLrMhybWRQfoobarbaz", isToken = true))

      import scala.concurrent.duration._

      val unsignedMessage = BroadcastMessage(authn, ReadQueryResultRequest("SHRINE", 180.seconds, localAuthn, 7923919416951966472L))

      val signedMessage = signerVerifier.sign(unsignedMessage, signingCertStrategy)

      (unsignedMessage eq signedMessage) should be(right = false)
      unsignedMessage should not equal (signedMessage)

      signedMessage.networkAuthn should equal(unsignedMessage.networkAuthn)
      signedMessage.request should equal(unsignedMessage.request)
      signedMessage.requestId should equal(unsignedMessage.requestId)

      unsignedMessage.signature should be(None)
      signedMessage.signature.isDefined should be(right = true)

      val sig = signedMessage.signature.get

      sig.timestamp should not be (null)
      sig.signedBy should equal(certCollection.myCertId.get)
      sig.value should not be (null)

      import scala.concurrent.duration._

      //The signed message should verify
      signerVerifier.verifySig(xmlRoundTrip(signedMessage), 1.hour) should be(right = true)
    }

    doTest(Attach)
    doTest(DontAttach)
  }

  private def getCertByAlias(alias: String) = certCollection.asInstanceOf[KeyStoreCertCollection].getX509Cert(alias).get

  @Test
  def testIsSignedByTrustedCA(): Unit = {

    import signerVerifier.isSignedByTrustedCA

    val signedByCa = getCertByAlias("test-cert")
    val notSignedByCa = getCertByAlias("spin-t1")

    isSignedByTrustedCA(signedByCa).get should be(right = true)
    isSignedByTrustedCA(notSignedByCa).isFailure should be(right = true)

    //TODO: Test case where isSignedByTrustedCA produces Success(false): 
    //where CA cert (param's issuer-DN) IS in our keystore, but X509Certificate.verify(PublicKey) returns false 
  }

  @Test
  def testObtainAndValidateSigningCert(): Unit = {
    import signerVerifier.obtainAndValidateSigningCert

    import scala.concurrent.duration._

    val unsignedMessage = BroadcastMessage(authn, ReadQueryResultRequest("SHRINE", 180.seconds, authn, 7923919416951966472L))

    //attached signing cert signed by known CA
    {
      val signedMessageWithAttachedSigner = signerVerifier.sign(unsignedMessage, SigningCertStrategy.Attach)

      val signerCert = obtainAndValidateSigningCert(signedMessageWithAttachedSigner.signature.get).get

      signerCert should equal(getCertByAlias("test-cert"))
    }

    //Known signer, no attached signing cert
    {
      val signedMessageWithoutAttachedSigner = signerVerifier.sign(unsignedMessage, SigningCertStrategy.DontAttach)

      val signerCert = obtainAndValidateSigningCert(signedMessageWithoutAttachedSigner.signature.get).get

      signerCert should equal(getCertByAlias("test-cert"))
    }

    //No attached cert, unknown signer: obtaining signing cert should fail
    {
      val signedMessage = signerVerifier.sign(unsignedMessage, SigningCertStrategy.DontAttach)

      val unknownSigner = CertId(new BigInteger("-1"))

      val signedMessageWithUnknownSigner = signedMessage.copy(signature = Some(signedMessage.signature.get.copy(signedBy = unknownSigner)))

      val signerCertAttempt = obtainAndValidateSigningCert(signedMessageWithUnknownSigner.signature.get)

      signerCertAttempt.isFailure should be(right = true)
    }
  }

  private def xmlRoundTrip(message: BroadcastMessage): BroadcastMessage = {
    val roundTripped = BroadcastMessage.fromXml(message.toXml).get

    roundTripped should equal(message)

    message
  }
}