package net.shrine.adapter

import net.shrine.protocol.AuthenticationInfo

/**
 * @author Andrew McMurry
 * @author clint
 * @since Jan 6, 2010
 * @since Nov 21, 2012 (Scala Port)
 */
final case class AdapterLockoutException(lockedOutAuthn: AuthenticationInfo,url:String) extends AdapterException {
  override def getMessage = s"AdapterLockoutException(domain=${lockedOutAuthn.domain}, username=${lockedOutAuthn.username}) on $url"
}
