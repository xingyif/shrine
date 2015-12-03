package net.shrine.adapter.service

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import javax.ws.rs.core.Response
import org.scalatest.mock.EasyMockSugar
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.I2b2AdminRequestHandler
import net.shrine.protocol.Credential
import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

/**
 * @author clint
 * @date Apr 3, 2013
 */
final class I2b2AdminResourceTest extends ShouldMatchersForJUnit with EasyMockSugar {
  @Test
  def testHandleBadInput {
    def doTestHandleBadInput(resourceMethod: I2b2AdminResource => String => Response) {
      val resource = I2b2AdminResource(mock[I2b2AdminRequestHandler], DefaultBreakdownResultOutputTypes.toSet)

      val fourHundredResponse = Response.status(400).build()

      //Just junk data
      {
        val resp = resourceMethod(resource)("sadlkhjksafhjksafhjkasgfgjskdfhsjkdfhgjsdfg")

        resp.getStatus should equal(fourHundredResponse.getStatus)
        resp.getEntity should be(null)
      }
      
      //A correctly-serialized request that we can't handle
      {
        val authn = AuthenticationInfo("d", "u", Credential("p", false))
        
        import scala.concurrent.duration._
        
        val resp = resourceMethod(resource)(DeleteQueryRequest("p", 123.milliseconds, authn, 99L).toI2b2String)

        resp.getStatus should equal(fourHundredResponse.getStatus)
        resp.getEntity should be(null)
      }
    }

    doTestHandleBadInput(_.doRequest)
  }
}