package net.shrine.adapter

import net.shrine.protocol.query.QueryDefinition

/**
 * @author Andrew McMurry
 * @author clint
 * @since ???
 * @since Nov 21, 2012 (Scala port)
 */
final case class AdapterMappingException(queryDefinition:QueryDefinition, message: String, cause: Throwable) extends AdapterException(message, cause)
