package net.shrine.protocol

import scala.concurrent.duration.Duration

/**
 * @author clint
 * @date Feb 18, 2014
 */
final case class I2b2AdminReadQueryDefinitionRequest(
    override val projectId: String,
    override val waitTime: Duration,
    override val authn: AuthenticationInfo,
    override val queryId: Long) extends AbstractReadQueryDefinitionRequest(projectId, waitTime, authn, queryId) with HandleableAdminShrineRequest {

  override def handleAdmin(handler: I2b2AdminRequestHandler, shouldBroadcast: Boolean) = handler.readQueryDefinition(this, shouldBroadcast)
}

object I2b2AdminReadQueryDefinitionRequest extends AbstractReadQueryDefinitionRequest.Companion(new I2b2AdminReadQueryDefinitionRequest(_, _, _, _))