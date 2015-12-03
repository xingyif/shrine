package net.shrine.adapter.components

import net.shrine.adapter.dao.I2b2AdminDao
import net.shrine.protocol.ReadI2b2AdminQueryingUsersRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.ReadI2b2AdminQueryingUsersResponse

/**
 * @author clint
 * @date Jan 10, 2014
 */
final case class I2b2AdminQueryingUsers(i2b2AdminDao: I2b2AdminDao) {
  def get(request: ReadI2b2AdminQueryingUsersRequest): ShrineResponse = {
    val users = i2b2AdminDao.findQueryingUsersByProjectId(request.projectIdToQueryFor)

    ReadI2b2AdminQueryingUsersResponse(users)
  }
}