package net.shrine.protocol.handlers

import net.shrine.protocol.ReadI2b2AdminPreviousQueriesRequest
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.ReadI2b2AdminQueryingUsersRequest

/**
 * @author clint
 * @date Apr 1, 2013
 */
trait ReadI2b2AdminPreviousQueriesHandler {
  def readI2b2AdminPreviousQueries(request: ReadI2b2AdminPreviousQueriesRequest, shouldBroadcast: Boolean): ShrineResponse
}