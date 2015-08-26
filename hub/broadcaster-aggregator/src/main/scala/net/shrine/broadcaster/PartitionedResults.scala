package net.shrine.broadcaster

import net.shrine.protocol.Result
import net.shrine.protocol.ErrorResponse

/**
 * @author clint
 * @date Dec 4, 2013
 */
final case class PartitionedResults(results: Iterable[Result], errors: Iterable[ErrorResponse])