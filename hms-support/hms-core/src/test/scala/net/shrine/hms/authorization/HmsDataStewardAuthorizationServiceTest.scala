package net.shrine.hms.authorization

import net.shrine.authorization.AuthorizationResult.{NotAuthorized, Authorized}
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.ApprovedTopic
import org.scalatest.mock.EasyMockSugar
import net.shrine.authentication.AuthenticationResult
import net.shrine.authentication.Authenticator
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.protocol.ReadApprovedQueryTopicsResponse

/**
 * @author Bill Simons
 * @since 1/30/12
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class HmsDataStewardAuthorizationServiceTest extends ShouldMatchersForJUnit {
  @Test
  def testIdentifyEcommonsUsername: Unit = {
    import HmsDataStewardAuthorizationService.identifyEcommonsUsername
    import AuthenticationResult._

    identifyEcommonsUsername(NotAuthenticated("", "", "")) should be(None)

    val ecommonsId = "foo"

    identifyEcommonsUsername(Authenticated("", ecommonsId)) should be(Some(ecommonsId))
  }

  import HmsDataStewardAuthorizationServiceTest._
  import scala.concurrent.duration._

  private val authn = AuthenticationInfo("d", "u", Credential("p", false))

  @Test
  def testReadApprovedEntriesNotAuthenticated {
    val service = HmsDataStewardAuthorizationService(null, NeverAuthenticatesAuthenticator)

    val result = service.readApprovedEntries(ReadApprovedQueryTopicsRequest("projectId", 0.minutes, authn, authn.username))

    val Left(errorResponse: ErrorResponse) = result

    errorResponse.errorMessage should not be (null)
  }

  @Test
  def testReadApprovedEntriesAuthenticated {
    val topic = ApprovedTopic(123L, "blarg")

    val ecommonsUsername = "abc123"

    val mockSheriffClient = MockSheriffClient(topics = Seq(topic))

    val service = HmsDataStewardAuthorizationService(mockSheriffClient, AlwaysAuthenticatesAuthenticator(ecommonsUsername))

    val result = service.readApprovedEntries(ReadApprovedQueryTopicsRequest("projectId", 0.minutes, authn, authn.username))

    val Right(ReadApprovedQueryTopicsResponse(Seq(actualTopic))) = result

    actualTopic should equal(topic)

    mockSheriffClient.Params.user should be(null)
    mockSheriffClient.Params.topicId should be(null)
    mockSheriffClient.Params.queryText should be(null)
    mockSheriffClient.Params.ecommonsUsername should be(ecommonsUsername)
  }

  @Test
  def testAuthorizeRunQueryRequestNotAuthenticated {
    val service = HmsDataStewardAuthorizationService(null, NeverAuthenticatesAuthenticator)

    def doTest(topicId: Option[String],topicName:Option[String]): Unit = {
      val result = service.authorizeRunQueryRequest(RunQueryRequest("projectId", 0.minutes, authn, topicId, topicName, Set.empty, QueryDefinition("foo", Term("foo"))))

      result.isAuthorized should be(false)
    }

    doTest(None,None)
    doTest(Some("topicId"),Some("Topic Name"))
  }

  @Test
  def testAuthorizeRunQueryRequestAuthenticated {

    def doTest(isAuthorized: Boolean, topicId: Option[String], topicName:Option[String]): Unit = {
      val ecommonsUsername = "abc123"
      val queryDef = QueryDefinition("foo", Term("foo"))

      val mockSheriffClient = MockSheriffClient(authorized = isAuthorized)

      val service = HmsDataStewardAuthorizationService(mockSheriffClient, AlwaysAuthenticatesAuthenticator(ecommonsUsername))

      val result = service.authorizeRunQueryRequest(RunQueryRequest("projectId", 0.minutes, authn, topicId, topicName, Set.empty, queryDef))

      val expectedIsAuthorized = isAuthorized && topicId.isDefined

      result.isAuthorized should be(expectedIsAuthorized)

      if (topicId.isDefined) {
        mockSheriffClient.Params.user should equal(ecommonsUsername)
        mockSheriffClient.Params.topicId should equal(topicId.get)
        mockSheriffClient.Params.queryText should equal(queryDef.toI2b2String)
        mockSheriffClient.Params.ecommonsUsername should be(null)
      } else {
        mockSheriffClient.Params.user should be(null)
        mockSheriffClient.Params.topicId should be(null)
        mockSheriffClient.Params.queryText should be(null)
        mockSheriffClient.Params.ecommonsUsername should be(null)
      }
    }

    doTest(true, Some("topic123"), Some("Topic Name"))
    doTest(false, Some("topic123"), Some("Topic Name"))
    doTest(false, Some("topic123"), None)
    doTest(true, None, None)
    doTest(false, None, None)
  }
}

object HmsDataStewardAuthorizationServiceTest {
  object NeverAuthenticatesAuthenticator extends Authenticator {
    override def authenticate(authn: AuthenticationInfo) = AuthenticationResult.NotAuthenticated(authn.domain, authn.username, "foo")
  }

  final case class AlwaysAuthenticatesAuthenticator(ecommonsUsername: String) extends Authenticator {
    override def authenticate(authn: AuthenticationInfo) = AuthenticationResult.Authenticated(authn.domain, ecommonsUsername)
  }

  final case class MockSheriffClient(authorized: Boolean = false, topics: Seq[ApprovedTopic] = Nil) extends SheriffClient {
    object Params {
      var ecommonsUsername: String = _

      var user: String = _
      var topicId: String = _
      var queryText: String = _
    }

    override def getApprovedEntries(ecommonsUsername: String): Seq[ApprovedTopic] = {
      Params.ecommonsUsername = ecommonsUsername

      topics
    }

    override def isAuthorized(user: String, topicId: String, queryText: String) = {
      Params.user = user
      Params.topicId = topicId
      Params.queryText = queryText

      if(authorized) Authorized(Some((topicId,"Mock Topic")))
      else NotAuthorized("Mock authorization failure")
    }
  }
}