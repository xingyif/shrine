package net.shrine.integration

import net.shrine.service.ShrineResource
import net.shrine.protocol.ShrineRequestHandler
import net.shrine.service.I2b2BroadcastResource
import net.shrine.protocol.I2b2RequestHandler
import net.shrine.service.ShrineService
import net.shrine.service.I2b2BroadcastService
import net.shrine.client.ShrineClient
import net.shrine.client.JerseyShrineClient
import net.shrine.protocol.AuthenticationInfo
import net.shrine.crypto.TrustParam
import net.shrine.client.Poster
import net.shrine.client.JerseyHttpClient
import net.shrine.broadcaster.BroadcasterClient
import net.shrine.protocol.DefaultBreakdownResultOutputTypes

object Hubs {
  val defaultShrineBasePath = ""
  val defaultI2b2BasePath = "i2b2/request"
  val defaultPort = 9997
  
  final case class Shrine(
      enclosingTest: AbstractHubAndSpokesTest, 
      broadcasterClient: Option[BroadcasterClient] = None, 
      override val basePath: String = defaultShrineBasePath, 
      override val port: Int = defaultPort) extends AbstractHubComponent[ShrineRequestHandler](enclosingTest, basePath, port) {
    
    override def resourceClass(handler: ShrineRequestHandler) = ShrineResource(handler)
    
    override lazy val makeHandler: ShrineRequestHandler = {
      import scala.concurrent.duration._
      
      ShrineService(
          "example.com",
          MockAuditDao,
          MockAuthenticator,
          MockQueryAuthorizationService,
          includeAggregateResult = false,
          signingBroadcastService(broadcasterClient.getOrElse(inJvmBroadcasterClient)),
          1.hour,
          Set.empty,
          false)
    }
    
    override def clientFor(projectId: String, networkAuthn: AuthenticationInfo): ShrineClient = new JerseyShrineClient(resourceUrl, projectId, networkAuthn, DefaultBreakdownResultOutputTypes.toSet, TrustParam.AcceptAllCerts)
  }
  
  final case class I2b2(
      enclosingTest: AbstractHubAndSpokesTest,
      broadcasterClient: Option[BroadcasterClient] = None, 
      override val basePath: String = defaultI2b2BasePath, 
      override val port: Int = defaultPort) extends AbstractHubComponent[I2b2RequestHandler](enclosingTest, basePath, port) {
    
    override def resourceClass(handler: I2b2RequestHandler) = I2b2BroadcastResource(handler, DefaultBreakdownResultOutputTypes.toSet)
    
    import scala.concurrent.duration._
    
    override lazy val makeHandler: I2b2RequestHandler = {
      I2b2BroadcastService(
          "example.com",
          MockAuditDao,
          MockAuthenticator,
          MockQueryAuthorizationService,
          includeAggregateResult = false,
          signingBroadcastService(broadcasterClient.getOrElse(inJvmBroadcasterClient)),
          1.hour,
          Set.empty)
    }
    
    override def clientFor(projectId: String, networkAuthn: AuthenticationInfo): ShrineClient = {
      I2b2ShrineClient(Poster(resourceUrl, JerseyHttpClient(TrustParam.AcceptAllCerts, 1.hour)), projectId, networkAuthn)
    }
  }
}