package net.shrine.protocol

import net.shrine.protocol.handlers.ReadI2b2AdminPreviousQueriesHandler
import net.shrine.protocol.handlers.ReadQueryDefinitionHandler
import net.shrine.protocol.handlers.RunHeldQueryHandler

/**
 * @author clint
 * @date Apr 1, 2013
 */
trait I2b2AdminRequestHandler extends 
	RunHeldQueryHandler with
	ReadI2b2AdminPreviousQueriesHandler with 
	ReadQueryDefinitionHandler[I2b2AdminReadQueryDefinitionRequest, ShrineResponse] {
  
  def readI2b2AdminQueryingUsers(request: ReadI2b2AdminQueryingUsersRequest, shouldBroadcast: Boolean): ShrineResponse

}