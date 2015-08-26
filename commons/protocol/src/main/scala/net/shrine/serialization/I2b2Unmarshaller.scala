package net.shrine.serialization

import xml.{XML, NodeSeq}
import net.shrine.protocol.AuthenticationInfo
import net.shrine.protocol.RequestHeader
import scala.concurrent.duration.Duration
import scala.util.Try
import net.shrine.util.NodeSeqEnrichments
import net.shrine.protocol.RequestType
import net.shrine.protocol.CrcRequestType
import net.shrine.util.XmlDateHelper


/**
 * @author Bill Simons
 * @date 3/23/11
 * @link http://cbmi.med.harvard.edu
 * @link http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @link http://www.gnu.org/licenses/lgpl.html
 */
trait I2b2Unmarshaller[+T] extends I2b2UnmarshallingHelpers {

  def fromI2b2(xmlString: String): T = fromI2b2(XML.loadString(xmlString))

  def fromI2b2(nodeSeq: NodeSeq): T
}