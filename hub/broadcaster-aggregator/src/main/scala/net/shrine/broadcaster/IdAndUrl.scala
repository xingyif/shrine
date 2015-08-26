package net.shrine.broadcaster

import java.net.URL

import net.shrine.protocol.NodeId

/**
 * @author clint
 * @since Dec 5, 2013
 */
final case class IdAndUrl(nodeId: NodeId, url: URL)