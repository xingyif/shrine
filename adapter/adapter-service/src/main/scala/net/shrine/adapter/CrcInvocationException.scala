package net.shrine.adapter

import net.shrine.protocol.ShrineRequest
import net.shrine.protocol.RunQueryRequest

/**
 * @author clint
 * @date Mar 31, 2014
 * 
 * NB: This is no longer a case class to allow overriding the defauly apply() implementation (boo!)
 */
final class CrcInvocationException private (val invokedUrl: String, val request: ShrineRequest, val rootCause: Throwable) extends AdapterException(rootCause) {
  override def getMessage = s"CrcInvocationException(invokedUrl='$invokedUrl', request=$request, rootCause=$rootCause)"
  
  override def toString = s"CrcInvocationException($invokedUrl, $request, $rootCause)"
  
  private lazy val members = Seq(invokedUrl, request, rootCause)
  
  override def hashCode = members.hashCode
  
  override def equals(other: Any): Boolean = other match {
    case that: CrcInvocationException => this.members == that.members
    case _ => false
  }
}

object CrcInvocationException {
  def unapply(e: CrcInvocationException): Option[(String, ShrineRequest, Throwable)] = {
    Some((e.invokedUrl, e.request, e.rootCause))
  }
  
  def apply(invokedUrl: String, request: ShrineRequest, rootCause: Throwable): CrcInvocationException = {
    val reqToUse = request match {
      case runQueryReq: RunQueryRequest => runQueryReq.elideAuthenticationInfo
      case _ => request
    }
    
    new CrcInvocationException(invokedUrl, reqToUse, rootCause)
  }
}
