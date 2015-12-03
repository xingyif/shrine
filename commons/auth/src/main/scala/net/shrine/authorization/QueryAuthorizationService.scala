package net.shrine.authorization

import net.shrine.protocol.ReadApprovedQueryTopicsRequest
import net.shrine.protocol.ReadApprovedQueryTopicsResponse
import net.shrine.protocol.RunQueryRequest
import net.shrine.protocol.ErrorResponse

/**
 * @author Bill Simons
 * @since Aug 24, 2010
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 * <p/>
 * NOTICE: This software comes with NO guarantees whatsoever and is
 * licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
trait QueryAuthorizationService {

  //Contact a data steward and either return an Authorized or a NotAuthorized or throw an exception
  @throws(classOf[AuthorizationException])
  def authorizeRunQueryRequest(request: RunQueryRequest): AuthorizationResult

  //Either read the approved topics from a data steward or have an error response.
  def readApprovedEntries(request: ReadApprovedQueryTopicsRequest): Either[ErrorResponse, ReadApprovedQueryTopicsResponse]
}
