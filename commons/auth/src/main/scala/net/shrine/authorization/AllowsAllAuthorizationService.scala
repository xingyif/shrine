package net.shrine.authorization

import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.RunQueryRequest
import AuthorizationResult.Authorized
import net.shrine.protocol.ErrorResponse

/**
 * @author Bill Simons
 * @since Aug 25, 2010
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 * <p/>
 * NOTICE: This software comes with NO guarantees whatsoever and is
 * licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
object AllowsAllAuthorizationService extends QueryAuthorizationService {
  override def authorizeRunQueryRequest(request: RunQueryRequest): AuthorizationResult = Authorized

  override def readApprovedEntries(request: ReadApprovedQueryTopicsRequest): Either[ErrorResponse, ReadApprovedQueryTopicsResponse] = {
    //TODO: This is a smell
    throw new UnsupportedOperationException
  }
}
