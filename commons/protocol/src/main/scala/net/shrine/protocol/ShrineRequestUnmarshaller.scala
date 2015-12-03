package net.shrine.protocol

import xml.NodeSeq
import net.shrine.serialization.XmlUnmarshaller
import scala.util.Try
import scala.concurrent.duration.Duration
import net.shrine.util.NodeSeqEnrichments

/**
 * @author Bill Simons
 * @date 3/30/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
trait ShrineRequestUnmarshaller {
  final def shrineHeader(xml: NodeSeq): Try[RequestHeader] = {
    for {
      waitTime <- shrineWaitTime(xml)
      authn <- shrineAuthenticationInfo(xml)
      projectId <- shrineProjectId(xml)
    } yield RequestHeader(projectId, waitTime, authn)
  }

  import NodeSeqEnrichments.Strictness._
  
  final def shrineProjectId(xml: NodeSeq): Try[String] = xml.withChild("projectId").map(_.text)

  final def shrineWaitTime(xml: NodeSeq): Try[Duration] = {
    import scala.concurrent.duration._
    
    xml.withChild("waitTimeMs").map(_.text.toLong.milliseconds)
  }

  final def shrineAuthenticationInfo(xml: NodeSeq): Try[AuthenticationInfo] = {
    xml.withChild(AuthenticationInfo.shrineXmlTagName).flatMap(AuthenticationInfo.fromXml)
  }
}
