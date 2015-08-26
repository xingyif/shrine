package net.shrine.adapter

import net.shrine.protocol.AuthenticationInfo

/**
 * @author Andrew McMurry
 * @author clint
 * @date Jan 6, 2010
 * @date Nov 21, 2012 (Scala Port)
 */
final class AdapterLockoutException(lockedOutAuthn: AuthenticationInfo) extends AdapterException {
  override def getMessage = s"AdapterLockoutException(domain=${lockedOutAuthn.domain}, username=${lockedOutAuthn.username})"
}
