package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Jan 13, 2014
 */
final class ReadI2b2AdminQueryingUsersResponseTest extends ShouldMatchersForJUnit {
  private val projectId1 = "some-pid-1"
  private val projectId2 = "some-pid-2"
    
  private val username1 = "some-uid-1"
  private val username2 = "some-uid-2"
    
  private val role1 = "some-role-1"
  private val role2 = "some-role-2"
  
  private val users = Seq(I2b2AdminUserWithRole(projectId1, username1, role1), I2b2AdminUserWithRole(projectId2, username2, role2))
    
  @Test
  def testI2b2MessageBody {
    def expectedSingleResult(pid: String, uid: String, rid: String) = s"<role><project_id>$pid</project_id><user_name>$uid</user_name><role>$rid</role></role>"
    
    val expected = s"<ns6:roles>${expectedSingleResult(projectId1, username1, role1)}${expectedSingleResult(projectId2, username2, role2)}</ns6:roles>"
      
    ReadI2b2AdminQueryingUsersResponse(users).i2b2Body.toString should equal(expected)
  }
  
  @Test
  def testToXml {
    def expectedSingleResult(pid: String, uid: String, rid: String) = s"<user><projectId>$pid</projectId><username>$uid</username><role>$rid</role></user>"
    
    val expected = s"<readI2b2AdminQueryingUsersResponse><users>${expectedSingleResult(projectId1, username1, role1)}${expectedSingleResult(projectId2, username2, role2)}</users></readI2b2AdminQueryingUsersResponse>"
    
    ReadI2b2AdminQueryingUsersResponse(users).toXmlString should equal(expected)
  }
  
  @Test
  def testFromI2b2 {
    val resp = ReadI2b2AdminQueryingUsersResponse(users)
    
    val i2b2Xml = resp.toI2b2
    
    val unmarshalled = ReadI2b2AdminQueryingUsersResponse.fromI2b2(i2b2Xml)
    
    unmarshalled should equal(resp)
  }
}