package net.shrine.integration

import net.shrine.authorization.QueryAuthorizationService
import net.shrine.protocol.RunQueryRequest
import net.shrine.authorization.AuthorizationResult
import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.ErrorResponse

/**
 * @author clint
 * @date Nov 27, 2013
 */
object MockQueryAuthorizationService extends QueryAuthorizationService {
  override def authorizeRunQueryRequest(request: RunQueryRequest): AuthorizationResult = AuthorizationResult.Authorized

  override def readApprovedEntries(request: ReadApprovedQueryTopicsRequest): Either[ErrorResponse, ReadApprovedQueryTopicsResponse] = ???
}