package net.shrine.authorization

import net.shrine.log.Loggable
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
object AllowsAllAuthorizationService extends QueryAuthorizationService with Loggable {
  override def authorizeRunQueryRequest(request: RunQueryRequest): AuthorizationResult = {
    if(request == null) {
      //todo clean out nulls
      val npe = new NullPointerException("request is null in AllowsAllAuthorizationService.authorizeRunQueryRequest()")
      npe.fillInStackTrace()
      warn(s"request is null in authorizeRunQueryRequest",npe)
      Authorized(None)
    }
    else {
      val topicIdAndName = (request.topicId, request.topicName) match {
        case (Some(id), Some(name)) => Option((id, name))
        case (None, None) => None
        //todo case _ =>
      }
      Authorized(topicIdAndName)
    }
  }

  override def readApprovedEntries(request: ReadApprovedQueryTopicsRequest): Either[ErrorResponse, ReadApprovedQueryTopicsResponse] = {
    //TODO: This is a smell
    throw new UnsupportedOperationException
  }
}
