package net.shrine.util

import org.junit.Test
import scala.xml.NodeSeq
import scala.util.Success

/**
 * @author clint
 * @date Feb 3, 2014
 */
final class NodeSeqEnrichmentsTest extends ShouldMatchersForJUnit {
  
  private val xml: NodeSeq = <foo><bar><baz>123</baz><nuh/></bar></foo>
  
  @Test
  def testHelpersChildren {
    import NodeSeqEnrichments.Helpers._
    
    <foo/>.children should equal(NodeSeq.Empty)
    
    val xml: NodeSeq = <foo><bar><baz>123</baz><nuh/></bar></foo>
   
    val Seq(childElem) = xml.children
      
    childElem should equal(<bar><baz>123</baz><nuh/></bar>)
    
    xml.children.children should equal(NodeSeq.fromSeq(Seq(<baz>123</baz>, <nuh/>)))
  }
  
  @Test
  def testWithChildNodeSeq {
    import NodeSeqEnrichments.Strictness._
    
    (xml withChild "glarg").isFailure should be(true)
    
    (xml withChild "bar" withChild "glarg").isFailure should be(true)
    
    val bazAttempt = xml withChild "bar" withChild "baz"
    
    val Success(Seq(bazElem)) = bazAttempt
    
    bazElem should equal(<baz>123</baz>)
  }
}