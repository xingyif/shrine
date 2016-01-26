package net.shrine.qep

import junit.framework.TestCase
import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import javax.ws.rs.core.Response
import org.scalatest.mock.EasyMockSugar
import net.shrine.protocol.ShrineRequestHandler
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.ErrorResponse
import net.shrine.protocol.I2b2RequestHandler
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 * @author clint
 * @date Mar 25, 2013
 */
final class I2b2BroadcastResourceTest extends ShouldMatchersForJUnit with EasyMockSugar {
  @Test
  def testHandleBadInput {
    def doTestHandleBadInput(resourceMethod: I2b2BroadcastResource => String => Response) {
      val resource = I2b2BroadcastResource(mock[I2b2RequestHandler], DefaultBreakdownResultOutputTypes.toSet)

      def checkIsErrorResponse(resp: Response) {
        val responseBody = resp.getEntity.asInstanceOf[String]
        
        val errorResponse = ErrorResponse.fromI2b2(responseBody)
        
        errorResponse.errorMessage should not be(null)
        errorResponse.errorMessage.take(5) should equal("Error")
      }
      
      //Just junk data
      {
        val resp = resourceMethod(resource)("sadlkhjksafhjksafhjkasgfgjskdfhsjkdfhgjsdfg")

        resp.getStatus should equal(200)

        checkIsErrorResponse(resp)
      }
      
      //A correctly-serialized request that we can't handle also counts as a bad request
      {
        val authn = AuthenticationInfo("d", "u", Credential("p", false))
        
        import scala.concurrent.duration._
        import ReadI2b2AdminPreviousQueriesRequest.Username._
        
        val resp = resourceMethod(resource)(ReadI2b2AdminPreviousQueriesRequest("p", 123.milliseconds, authn, Exactly("@"), "foo", 20, None).toI2b2String)

        resp.getStatus should equal(200)
        
        checkIsErrorResponse(resp)
      }
    }

    doTestHandleBadInput(_.doPDORequest)

    doTestHandleBadInput(_.doRequest)
  }
}