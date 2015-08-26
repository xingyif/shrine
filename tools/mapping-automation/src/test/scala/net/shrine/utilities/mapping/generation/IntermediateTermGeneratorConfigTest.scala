package net.shrine.utilities.mapping.generation

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Jul 21, 2014
 */
final class IntermediateTermGeneratorConfigTest extends ShouldMatchersForJUnit {
  @Test
  def testConstructorDefaults: Unit = {
    val config = IntermediateTermGeneratorConfig("foo", "bar")
    
    config.hLevelToStopAt should be(None)
  }
  
  def testFromCommandLineArgs: Unit = {
    import IntermediateTermGeneratorConfig.fromCommandLineArgs
    
    def args(as: String*) = IntermediateTermGeneratorArgParser(as)
    
    fromCommandLineArgs(args("-i", "foo")) should be(None)
    fromCommandLineArgs(args("-o", "bar")) should be(None)
    
    fromCommandLineArgs(args("--input-file", "foo")) should be(None)
    fromCommandLineArgs(args("--output-file", "foo")) should be(None)
     
    fromCommandLineArgs(args("-i", "foo", "-o", "bar")) should be(IntermediateTermGeneratorConfig("foo", "bar", None))
    fromCommandLineArgs(args("--input-file", "foo", "--output-file", "bar")) should be(IntermediateTermGeneratorConfig("foo", "bar", None))
    
    fromCommandLineArgs(args("-i", "foo", "-o", "bar", "-h", "42")) should be(IntermediateTermGeneratorConfig("foo", "bar", Some(42)))
    fromCommandLineArgs(args("--input-file", "foo", "--output-file", "bar", "--min-hlevel", "42")) should be(IntermediateTermGeneratorConfig("foo", "bar", Some(42)))
  }
}