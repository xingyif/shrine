package net.shrine.qep

import org.junit.Test
import org.scalatest.mock.EasyMockSugar
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.qep.dao.AbstractAuditDaoTest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.ReadQueryInstancesRequest
import net.shrine.protocol.ReadQueryInstancesResponse
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.query.QueryDefinition
import net.shrine.protocol.query.Term
import net.shrine.authorization.AuthorizationResult
import net.shrine.authentication.Authenticator
import net.shrine.authentication.AuthenticationResult
import net.shrine.authentication.NotAuthenticatedException
import net.shrine.protocol.ErrorResponse

/**
 * @author Bill Simons
 * @author Clint Gilbert
 * @since 3/30/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
final class QepServiceTest extends AbstractAuditDaoTest with EasyMockSugar {

  import scala.concurrent.duration._

/*
  @Test
  def testReadQueryInstances() {
    val projectId = "foo"
    val queryId = 123L
    val authn = AuthenticationInfo("some-domain", "some-username", Credential("blarg", isToken = false))
    
    val req = ReadQueryInstancesRequest(projectId, 1.millisecond, authn, queryId)

    val service = QepService("example.com",null, AllowsAllAuthenticator, null, includeAggregateResult = true, null, null, Set.empty,collectQepAudit = false)

    val response = service.readQueryInstances(req).asInstanceOf[ReadQueryInstancesResponse]

    response should not be (null)
    response.groupId should equal(projectId)
    response.queryMasterId should equal(queryId)
    response.userId should equal(authn.username)

    val Seq(instance) = response.queryInstances

    instance.startDate should not be (null)
    instance.endDate should not be (null)
    instance.startDate should equal(instance.endDate)
    instance.groupId should equal(projectId)
    instance.queryInstanceId should equal(queryId.toString)
    instance.queryMasterId should equal(queryId.toString)
    instance.userId should equal(authn.username)
  }
  */

  private val authn = AuthenticationInfo("some-domain", "some-user", Credential("some-password", isToken = false))
  private val projectId = "projectId"
  private val queryDef = QueryDefinition("yo", Term("foo"))
  private val request = RunQueryRequest(projectId, 1.millisecond, authn, Some("topicId"), Some("Topic Name"), Set.empty, queryDef)

  @Test
  def testRunQueryAggregatorFor() {

    def doTestRunQueryAggregatorFor(addAggregatedResult: Boolean) {
      val service = QepService("example.com",null, null, null, addAggregatedResult, null, null, Set.empty,collectQepAudit = false)

      val aggregator = service.runQueryAggregatorFor(request)

      aggregator should not be (null)

      aggregator.queryId should be(-1L)
      aggregator.groupId should be(projectId)
      aggregator.userId should be(authn.username)
      aggregator.requestQueryDefinition should be(queryDef)
      aggregator.addAggregatedResult should be(addAggregatedResult)
    }

    doTestRunQueryAggregatorFor(true)
    doTestRunQueryAggregatorFor(false)
  }

  @Test
  def testAuditTransactionally() = afterMakingTables {
    def doTestAuditTransactionally(shouldThrow: Boolean) {
      val service = QepService("example.com",auditDao, null, null, includeAggregateResult = true, null, null, Set.empty,collectQepAudit = false)

      if (shouldThrow) {
        intercept[Exception] {
          service.auditTransactionally(request)(throw new Exception)
        }
      } else {
        val x = 1

        val actual = service.auditTransactionally(request)(x)

        actual should be(x)
      }

      //We should have recorded an audit entry no matter what
      val Seq(entry) = auditDao.findRecentEntries(1)

      entry.domain should be(authn.domain)
      entry.username should be(authn.username)
      entry.project should be(projectId)
      entry.queryText should be(Some(queryDef.toI2b2String))
      entry.queryTopic should be(request.topicId)
      entry.time should not be (null)
    }

    doTestAuditTransactionally(false)
    doTestAuditTransactionally(true)
  }

  import QepServiceTest._

  /*
  @Test
  def testAfterAuthenticating() {
    def doTestAfterAuthenticating(shouldAuthenticate: Boolean) {
      val service = QepService("example.com",auditDao, new MockAuthenticator(shouldAuthenticate), new MockAuthService(true), includeAggregateResult = true, null, null, Set.empty,collectQepAudit = false)

      if (shouldAuthenticate) {
        var foo = false

        service.authenticateAndThen(request) { _ =>
          foo = true
        }
        
        foo should be(right = true)
      } else {
        intercept[NotAuthenticatedException] {
          service.authenticateAndThen(request) { _ => () }
        }
      }
    }

    doTestAfterAuthenticating(true)
    doTestAfterAuthenticating(false)
  }
*/
  @Test
  def testAfterAuditingAndAuthorizing() = afterMakingTables {

    def doAfterAuditingAndAuthorizing(shouldBeAuthorized: Boolean, shouldThrow: Boolean) {
      val service = QepService("example.com",auditDao, AllowsAllAuthenticator, new MockAuthService(shouldBeAuthorized), includeAggregateResult = true, null, null, Set.empty,collectQepAudit = false)

      if (shouldThrow || !shouldBeAuthorized) {
        intercept[Exception] {
          service.auditAuthorizeAndThen(request)(request => throw new Exception)
        }
      } else {
        val x = 1

        val actual = service.auditAuthorizeAndThen(request)(request => x)

        actual should be(x)
      }

      //We should have recorded an audit entry no matter what
      val Seq(entry) = auditDao.findRecentEntries(1)

      entry.domain should be(authn.domain)
      entry.username should be(authn.username)
      entry.project should be(projectId)
      entry.queryText should be(Some(queryDef.toI2b2String))
      entry.queryTopic should be(request.topicId)
      entry.time should not be (null)
    }

    doAfterAuditingAndAuthorizing(shouldBeAuthorized = true, shouldThrow = true)
    doAfterAuditingAndAuthorizing(shouldBeAuthorized = true, shouldThrow = false)
    doAfterAuditingAndAuthorizing(shouldBeAuthorized = false, shouldThrow = true)
    doAfterAuditingAndAuthorizing(shouldBeAuthorized = false, shouldThrow = false)
  }
}

object QepServiceTest {
  final class MockAuthenticator(shouldAuthenticate: Boolean) extends Authenticator {
    override def authenticate(authn: AuthenticationInfo): AuthenticationResult = {
      if (shouldAuthenticate) { AuthenticationResult.Authenticated(authn.domain, authn.username) }
      else { AuthenticationResult.NotAuthenticated(authn.domain, authn.username, "blarg") }
    }
  }

  final class MockAuthService(shouldWork: Boolean) extends QueryAuthorizationService {
    def authorizeRunQueryRequest(request: RunQueryRequest): AuthorizationResult = {
      if (shouldWork) {
        val topicIdAndName = (request.topicId,request.topicName) match {
          case (Some(id),Some(name)) => Some((id,name))
          case (None,None) => None
        }
        AuthorizationResult.Authorized(topicIdAndName)}
      else { AuthorizationResult.NotAuthorized("blarg") }
    }

    def readApprovedEntries(request: ReadApprovedQueryTopicsRequest): Either[ErrorResponse, ReadApprovedQueryTopicsResponse] = ???
  }
}