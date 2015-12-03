package net.shrine.utilities.batchquerier.components

import net.shrine.client.JerseyShrineClient
import net.shrine.utilities.batchquerier.HasBatchQuerierConfig
import net.shrine.utilities.batchquerier.HasShrineClient
import net.shrine.utilities.batchquerier.HasTrustParam

/**
 * @author clint
 * @date Sep 6, 2013
 */
trait JerseyShrineClientComponent extends HasShrineClient { self: HasBatchQuerierConfig with HasTrustParam =>
  lazy val client = new JerseyShrineClient(config.shrineUrl, config.projectId, config.authorization, config.breakdownTypes, trustParam)
}