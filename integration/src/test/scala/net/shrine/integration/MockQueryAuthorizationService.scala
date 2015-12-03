package net.shrine.integration

import net.shrine.authorization.AuthorizationResult.Authorized
import net.shrine.authorization.QueryAuthorizationService
import net.shrine.protocol.RunQueryRequest
import net.shrine.authorization.AuthorizationResult
import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.ErrorResponse

/**
 * @author clint
 * @since Nov 27, 2013
 */
object MockQueryAuthorizationService extends QueryAuthorizationService {
  override def authorizeRunQueryRequest(request: RunQueryRequest): AuthorizationResult = {
    val topicIdAndName = (request.topicId,request.topicName) match {
      case (Some(id),Some(name)) => Some((id,name))
      case (None,None) => None
      case (Some(id),None) => None
    }
    Authorized(topicIdAndName)
  }

  override def readApprovedEntries(request: ReadApprovedQueryTopicsRequest): Either[ErrorResponse, ReadApprovedQueryTopicsResponse] = ???
}