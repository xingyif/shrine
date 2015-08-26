package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Jan 13, 2014
 */
final class I2b2AdminUserWithRoleTest extends ShouldMatchersForJUnit {
  val projectId = "some-project-id"
  val username = "some-username"
  val role = "some-role"
  
  @Test
  def testToI2b2 {
    val expected = s"<role><project_id>$projectId</project_id><user_name>$username</user_name><role>$role</role></role>"
    
    I2b2AdminUserWithRole(projectId, username, role).toI2b2String should equal(expected)
  }
  
  @Test
  def testToXml {
    val expected = s"<user><projectId>$projectId</projectId><username>$username</username><role>$role</role></user>"
    
    I2b2AdminUserWithRole(projectId, username, role).toXmlString should equal(expected)
  }
}