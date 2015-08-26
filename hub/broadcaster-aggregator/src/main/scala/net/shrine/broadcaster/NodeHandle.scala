package net.shrine.broadcaster

import net.shrine.protocol.NodeId
import net.shrine.adapter.client.AdapterClient

/**
 * @author clint
 * @date Nov 15, 2013
 */
final case class NodeHandle(nodeId: NodeId, client: AdapterClient)