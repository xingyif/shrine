package net.shrine.protocol

import scala.concurrent.duration.Duration
import net.shrine.util.XmlUtil
import net.shrine.serialization.I2b2Unmarshaller
import scala.xml.NodeSeq
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.shrine.serialization.I2b2UnmarshallingHelpers

/**
 * @author clint
 * @date Jan 10, 2014
 */
final case class ReadI2b2AdminQueryingUsersRequest(override val projectId: String, override val waitTime: Duration, override val authn: AuthenticationInfo, projectIdToQueryFor: String) extends ShrineRequest(projectId, waitTime, authn) with HandleableAdminShrineRequest {

  override val requestType = RequestType.ReadI2b2AdminQueryingUsers
  
  override def handleAdmin(handler: I2b2AdminRequestHandler, shouldBroadcast: Boolean): ShrineResponse = handler.readI2b2AdminQueryingUsers(this, shouldBroadcast)

  protected override def i2b2MessageBody = XmlUtil.stripWhitespace {
    <message_body>
      <ns99:get_all_role>
        <project_id>{ projectIdToQueryFor }</project_id>
      </ns99:get_all_role>
    </message_body>
  }

  override def toXml = XmlUtil.stripWhitespace {
    <readAdminQueryingUsers>
	  { headerFragment }
      <projectId>{ projectIdToQueryFor }</projectId>
    </readAdminQueryingUsers>
  }
}

object ReadI2b2AdminQueryingUsersRequest extends I2b2XmlUnmarshaller[ReadI2b2AdminQueryingUsersRequest] with ShrineXmlUnmarshaller[ReadI2b2AdminQueryingUsersRequest] with ShrineRequestUnmarshaller with I2b2UnmarshallingHelpers {
  override def fromXml(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadI2b2AdminQueryingUsersRequest] = {
    def textIn(tagName: String) = (xml \ tagName).text.trim

    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      projectIdToQueryFor <- Try(textIn("projectId"))
      projectId <- shrineProjectId(xml)
    } yield {
      ReadI2b2AdminQueryingUsersRequest(projectId, waitTime, authn, projectIdToQueryFor)
    }
  }
  
  override def fromI2b2(breakdownTypes: Set[ResultOutputType])(xml: NodeSeq): Try[ReadI2b2AdminQueryingUsersRequest] = {
    import NodeSeqEnrichments.Strictness._

    for {
      projectId <- i2b2ProjectId(xml)
      waitTime <- i2b2WaitTime(xml)
      authn <- i2b2AuthenticationInfo(xml)
      projectIdToQueryFor <- (xml withChild "message_body" withChild "get_all_role" withChild "project_id").map(_.text.trim)
    } yield {
      ReadI2b2AdminQueryingUsersRequest(projectId, waitTime, authn, projectIdToQueryFor)
    }
  }
}