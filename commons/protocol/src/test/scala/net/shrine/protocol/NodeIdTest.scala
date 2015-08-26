package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 3, 2013
 */
final class NodeIdTest extends ShouldMatchersForJUnit {
  @Test
  def testUnknown {
    NodeId.Unknown.name should be("Unknown")
  }
  
  @Test
  def testToXml {
    NodeId("foo").toXmlString should equal("<nodeId><name>foo</name></nodeId>")
  }
  
  @Test
  def testFromXml {
    NodeId.fromXml("<nodeId><name>foo</name></nodeId>").get should equal(NodeId("foo"))
    
    NodeId.fromXml(Nil).isFailure should be(true)
    
    NodeId.fromXml(<foo/>).isFailure should be(true)
  }
  
  @Test
  def testXmlRoundTrip {
    val nodeId = NodeId("foo")
    
    NodeId.fromXml(nodeId.toXml).get should equal(nodeId)
  }
}