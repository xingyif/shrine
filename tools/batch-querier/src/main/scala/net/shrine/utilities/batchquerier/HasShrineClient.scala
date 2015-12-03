package net.shrine.utilities.batchquerier

import net.shrine.client.ShrineClient

/**
 * @author clint
 * @date Sep 6, 2013
 */
trait HasShrineClient {
  val client: ShrineClient
}