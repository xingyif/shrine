package net.shrine.authorization

import java.net.URL

import net.shrine.authorization.AuthorizationResult.{NotAuthorized, Authorized}
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import spray.http.HttpResponse

import spray.http.StatusCodes.{OK,UnavailableForLegalReasons,BadRequest,Unauthorized}
/**
 * @author david 
 * @since 4/6/15
 */
class StewardQueryAuthorizationServiceTest extends ShouldMatchersForJUnit {

  val baseUrl = new URL("https://localhost:6443")

  @Test
  def testAuthorizeRunQueryRequest {
    val stewardQueryAuthorizer = StewardQueryAuthorizationService("qep","trustme",baseUrl)
    val fineResponse = HttpResponse(OK)
    stewardQueryAuthorizer.interpretAuthorizeRunQueryResponse(fineResponse) should be (Authorized)
  }

  @Test
  def testDoNotAuthorizeRunQueryRequest {
    val stewardQueryAuthorizer = StewardQueryAuthorizationService("qep","trustme",baseUrl)
    val rejectedResponse = HttpResponse(UnavailableForLegalReasons)
    stewardQueryAuthorizer.interpretAuthorizeRunQueryResponse(rejectedResponse) should not be (Authorized)
  }

  @Test
  def testBrokenAuthorizeRunQueryRequest {
    val stewardQueryAuthorizer = StewardQueryAuthorizationService("qep","trustme",baseUrl)
    val rejectedResponse = HttpResponse(BadRequest)
    intercept[AuthorizationException] {
      stewardQueryAuthorizer.interpretAuthorizeRunQueryResponse(rejectedResponse) should not be (Authorized)
    }
  }

}