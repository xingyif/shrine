package net.shrine.ont.data

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test
import net.shrine.client.OntClient

/**
 * @author clint
 * @date Jan 28, 2014
 */
final class OntCellOntologyMetadataTest extends ShouldMatchersForJUnit {
  @Test
  def testOntologyVersion {
    final case class MockOntClient(toReturn: Set[String]) extends OntClient {
      var lastParentTerm: Option[String] = None
      
      override def childrenOf(parent: String): Set[String] = {
        lastParentTerm = Option(parent)
        
        toReturn
      }
    }  
    
    {
      val metadata = new OntClientOntologyMetadata(MockOntClient(Set.empty))
      
      metadata.ontologyVersion should equal("UNKNOWN")
    }
    
    {
      val metadata = new OntClientOntologyMetadata(MockOntClient(Set("""\\SHRINE\SHRINE\1.2.3""")))
      
      metadata.ontologyVersion should equal("1.2.3")
    }
    
    {
      val metadata = new OntClientOntologyMetadata(MockOntClient(Set("""\\SHRINE\SHRINE\1.2.3\""")))
      
      metadata.ontologyVersion should equal("1.2.3")
    }
    
    {
      val metadata = new OntClientOntologyMetadata(MockOntClient(Set("""\\FOO\Bar\1.2.3\""")))
      
      metadata.ontologyVersion should equal("1.2.3")
    }
  }
  
  @Test
  def testDropTrailingSlash {
    import OntClientOntologyMetadata._
    
    dropTrailingSlash("") should equal("")
    dropTrailingSlash("""\""") should equal("")
    dropTrailingSlash("""\\""") should equal("""\""")
    
    dropTrailingSlash("""foo\""") should equal("""foo""")
    dropTrailingSlash("foo") should equal("foo")
  }
}