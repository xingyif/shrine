package net.shrine.adapter


import net.shrine.protocol.RunQueryRequest

/**
 * @author Andrew McMurry
 * @author clint
 * @since ???
 * @since Nov 21, 2012 (Scala port)
 */
final case class AdapterMappingException(runQueryRequest: RunQueryRequest, message: String, cause: Throwable) extends
  AdapterException(s"$message for request ${runQueryRequest.elideAuthenticationInfo}", cause)
