package net.shrine.protocol

import scala.xml.NodeSeq
import net.shrine.util.XmlUtil
import net.shrine.serialization.I2b2Unmarshaller
import net.shrine.serialization.XmlUnmarshaller

/**
 * @author clint
 * @date Jan 10, 2014
 */
final case class ReadI2b2AdminQueryingUsersResponse(users: Seq[I2b2AdminUserWithRole]) extends ShrineResponse {
  
  //For tests
  private[protocol] def i2b2Body: NodeSeq = i2b2MessageBody
  
  override protected def i2b2MessageBody: NodeSeq = XmlUtil.stripWhitespace {
    <ns6:roles>
	  { users.toSeq.map(_.toI2b2) }
	</ns6:roles>
  }
  
  override def toXml: NodeSeq = XmlUtil.stripWhitespace {
    <readI2b2AdminQueryingUsersResponse>
      <users>
	    { users.toSeq.map(_.toXml) }
	  </users>
	</readI2b2AdminQueryingUsersResponse>
  }
}

object ReadI2b2AdminQueryingUsersResponse extends I2b2Unmarshaller[ReadI2b2AdminQueryingUsersResponse] {
  //NB: Only needed for tests :/
  override def fromI2b2(xml: NodeSeq): ReadI2b2AdminQueryingUsersResponse = {
    //NB: Fail fast
    require((xml \ "response_header" \ "result_status" \ "status" \ "@type").text == "DONE")
    
    val usersWithRoles = (xml \ "message_body" \ "roles" \ "role").map { userWithRoleXml =>
      val projectId = (userWithRoleXml \ "project_id").text.trim
      val username = (userWithRoleXml \ "user_name").text.trim
      val role = (userWithRoleXml \ "role").text.trim

      I2b2AdminUserWithRole(projectId, username, role)
    }

    ReadI2b2AdminQueryingUsersResponse(usersWithRoles)
  }
}