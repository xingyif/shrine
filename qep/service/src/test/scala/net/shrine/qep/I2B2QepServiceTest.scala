package net.shrine.qep

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.authentication.Authenticator
import net.shrine.protocol.AuthenticationInfo
import net.shrine.authentication.AuthenticationResult
import net.shrine.protocol.ReadResultOutputTypesRequest
import net.shrine.protocol.Credential
import net.shrine.protocol.ReadResultOutputTypesResponse
import net.shrine.protocol.ResultOutputType
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 * @author clint
 * @since Oct 23, 2014
 */
final class I2B2QepServiceTest extends ShouldMatchersForJUnit {

  private val knownUsername = "some-user"
  private val unknownUsername = "some-unknown-user"

  import scala.concurrent.duration._
/*
  @Test
  def testReadResultOutputTypes(): Unit = {
    val authenticator: Authenticator = new Authenticator {
      override def authenticate(authn: AuthenticationInfo): AuthenticationResult = {
        if (authn.username == knownUsername) {
          AuthenticationResult.Authenticated(authn.domain, authn.username)
        } else {
          AuthenticationResult.NotAuthenticated(authn.domain, authn.username, "blarg")
        }
      }
    }
    
    val breakdownResultOutputTypes = DefaultBreakdownResultOutputTypes.toSet

    val service = I2b2QepService("example.com",null, authenticator, null, includeAggregateResult = true, null, 1.day, breakdownResultOutputTypes,false)

    {
      val req = ReadResultOutputTypesRequest("project-id", 1.minute, AuthenticationInfo("d", knownUsername, Credential("foo", isToken = false)))

      val resp = service.readResultOutputTypes(req)

      resp.asInstanceOf[ReadResultOutputTypesResponse].outputTypes should equal(ResultOutputType.nonErrorTypes ++ breakdownResultOutputTypes)
    }

    {
      val req = ReadResultOutputTypesRequest("project-id", 1.minute, AuthenticationInfo("d", unknownUsername, Credential("foo", isToken = false)))

      intercept[Exception] {
        service.readResultOutputTypes(req)
      }
    }
  }
  */
}