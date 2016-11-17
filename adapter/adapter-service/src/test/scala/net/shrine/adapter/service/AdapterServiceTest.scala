package net.shrine.adapter.service

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.protocol.NodeId
import net.shrine.protocol.DeleteQueryResponse
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.Credential
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.DeleteQueryRequest
import net.shrine.adapter.AdapterMap
import net.shrine.adapter.DeleteQueryAdapter
import net.shrine.adapter.dao.MockAdapterDao
import net.shrine.protocol.RenameQueryRequest
import net.shrine.protocol.ErrorResponse
import net.shrine.crypto.{NewTestKeyStore, SignerVerifierAdapter, SigningCertStrategy}

/**
 * @author clint
 * @date Dec 9, 2013
 */
final class AdapterServiceTest extends ShouldMatchersForJUnit {
  import scala.concurrent.duration._

  private val nodeId = NodeId("foo")

  private val resp = DeleteQueryResponse(12345)

  private val queryTime = 100.milliseconds

  @Test
  def testTime {

    val result = AdapterService.time(nodeId) {
      Thread.sleep(queryTime.toMillis)

      resp
    }

    result.origin should equal(nodeId)
    result.response should equal(resp)
    (result.elapsed >= queryTime) should be(true)
  }

  @Test
  def testHandleRequest {
    val signerVerifier = SignerVerifierAdapter(NewTestKeyStore.certCollection)

    val authn = AuthenticationInfo("d", "u", Credential("p", false))
    
    val masterId = 12345

    val req = DeleteQueryRequest("project-id", 1.second, authn, masterId)

    val unsignedMessage = BroadcastMessage(authn, req)

    val signedMessage = signerVerifier.sign(unsignedMessage, SigningCertStrategy.Attach)

    val adapterMap = new AdapterMap(Map(req.requestType -> new DeleteQueryAdapter(MockAdapterDao)))

    val service = new AdapterService(nodeId, signerVerifier, 1.minute, adapterMap)

    val errorResult = service.handleRequest(unsignedMessage)
    
    errorResult.origin should equal(nodeId)
    errorResult.response.isInstanceOf[ErrorResponse] should be(true)

    //Unhandled query types should give a wrapped ErrorResponse
    {
      val unhandledReq = RenameQueryRequest("project-id", 1.second, authn, masterId, "foo")

      val resultForUnhandledQueryType = service.handleRequest(signerVerifier.sign(BroadcastMessage(authn, unhandledReq), SigningCertStrategy.DontAttach))

      //resultForUnhandledQueryType.elapsed should equal(0.milliseconds)
      resultForUnhandledQueryType.origin should equal(nodeId)
      resultForUnhandledQueryType.response.getClass should equal(classOf[ErrorResponse])
    }

    //Legit requests should work
    {
      val result = service.handleRequest(signedMessage)
      
      result.elapsed should not be(null)
      result.origin should equal(nodeId)
      result.response.asInstanceOf[DeleteQueryResponse].queryId should equal(masterId)
    }
  }
}
