package net.shrine.adapter.components

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.adapter.HasI2b2AdminDao
import net.shrine.adapter.service.CanLoadTestData
import net.shrine.adapter.AdapterTestHelpers
import net.shrine.adapter.dao.squeryl.AbstractSquerylAdapterTest
import net.shrine.protocol.ReadI2b2AdminQueryingUsersRequest
import net.shrine.protocol.ReadI2b2AdminQueryingUsersResponse

/**
 * @author clint
 * @date Jan 13, 2014
 */
final class I2b2AdminQueryingUsersTest extends AbstractSquerylAdapterTest with AdapterTestHelpers with ShouldMatchersForJUnit with CanLoadTestData with HasI2b2AdminDao {
  
  private val i2b2AdminQueryingUsers = I2b2AdminQueryingUsers(i2b2AdminDao)

  private def get(req: ReadI2b2AdminQueryingUsersRequest): ReadI2b2AdminQueryingUsersResponse = {
    val resp = i2b2AdminQueryingUsers.get(req).asInstanceOf[ReadI2b2AdminQueryingUsersResponse]
    
    resp.users.forall(_.role == "USER") should be(true)
    
    resp.users.forall(_.projectId == shrineProjectId) should be(true)
    
    resp
  }

  @Test
  def testGet = afterLoadingTestData {
    val request = ReadI2b2AdminQueryingUsersRequest(projectId, waitTime, authn, "foo")

    val resp = get(request)

    resp.users.map(_.username).toSet should equal(Set(authn.username, authn2.username))
  }
  
  @Test
  def testGetNoTestDataLoaded = afterCreatingTables {
    val request = ReadI2b2AdminQueryingUsersRequest(projectId, waitTime, authn, "foo")

    val resp = get(request)

    resp.users should equal(Nil)
  }
}