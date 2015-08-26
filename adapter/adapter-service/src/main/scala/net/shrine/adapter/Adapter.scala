package net.shrine.adapter

import net.shrine.log.Loggable
import net.shrine.protocol.BroadcastMessage
import net.shrine.protocol.ErrorResponse
import net.shrine.serialization.XmlMarshaller
import net.shrine.util.StackTrace
import net.shrine.protocol.ShrineResponse
import net.shrine.protocol.BaseShrineResponse
import net.shrine.protocol.AuthenticationInfo

/**
 * @author Bill Simons
 * @since 4/8/11
 * @see http://cbmi.med.harvard.edu
 * @see http://chip.org
 *       <p/>
 *       NOTICE: This software comes with NO guarantees whatsoever and is
 *       licensed as Lgpl Open Source
 * @see http://www.gnu.org/licenses/lgpl.html
 */
abstract class Adapter extends Loggable {
  
  final def perform(message: BroadcastMessage): BaseShrineResponse = {
    val shrineResponse = try {
      processRequest(message)
    } catch {
      case e: AdapterLockoutException => {
        val AuthenticationInfo(domain, username, _) = message.request.authn
        
        warn(s"User '$domain:$username' is locked out")
        
        errorResponseFrom(e)
      }
      case e @ CrcInvocationException(invokedCrcUrl, request, cause) => {
        error(s"Error invoking the CRC at '$invokedCrcUrl' with request $request . Root cause: ", cause)
        
        errorResponseFrom(e)
      }
      case e: AdapterMappingException => {
        warn(s"Error mapping query terms: ${e.getMessage}", e)
        
        errorResponseFrom(e)
      } 
      case e: Exception => {
        //for now we'll warn on all errors and work towards more specific logging later
        def messageXml = Option(message).map(_.toXmlString).getOrElse("(Null message)")
        
        warn(s"Exception $e in Adapter with stack trace:\r\n${ StackTrace.stackTraceAsString(e) } caused on request\r\n $messageXml", e)
        
        errorResponseFrom(e)
      }
    }

    shrineResponse
  }

  private def errorResponseFrom(e: Throwable) = ErrorResponse(e.getMessage)
  
  protected[adapter] def processRequest(message: BroadcastMessage): BaseShrineResponse
  
  //NOOP, may be overridden by subclasses
  def shutdown(): Unit = ()
}