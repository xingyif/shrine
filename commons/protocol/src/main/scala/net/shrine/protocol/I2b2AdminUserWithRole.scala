package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.I2b2Marshaller
import net.shrine.serialization.XmlMarshaller

/**
 * @author clint
 * @date Jan 10, 2014
 *
 * A class to represent i2b2 users with roles, as needed by the i2b2 admin API call that returns
 * all users that queried an Adapter.
 */
final case class I2b2AdminUserWithRole(projectId: String, username: String, role: String) extends I2b2Marshaller with XmlMarshaller {

  override def toI2b2: NodeSeq = XmlUtil.stripWhitespace {
    <role>
      <project_id>{ projectId }</project_id>
      <user_name>{ username }</user_name>
      <role>{ role }</role>
    </role>
  }
  
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <user>
      <projectId>{ projectId }</projectId>
      <username>{ username }</username>
      <role>{ role }</role>
    </user>
  }
}