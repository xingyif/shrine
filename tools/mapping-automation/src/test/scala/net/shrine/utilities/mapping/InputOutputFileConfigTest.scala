package net.shrine.utilities.mapping

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Jul 17, 2014
 */
final class InputOutputFileConfigTest extends ShouldMatchersForJUnit {
  @Test
  def testFromCommandLineArgs: Unit = {
    import InputOutputFileConfig.fromCommandLineArgs
    
    fromCommandLineArgs(InputOutputFileArgParser(Seq.empty)) should be(None)
    
    fromCommandLineArgs(InputOutputFileArgParser(Seq("-i", "foo"))) should be(None)
    fromCommandLineArgs(InputOutputFileArgParser(Seq("-o", "foo"))) should be(None)
    
    fromCommandLineArgs(InputOutputFileArgParser(Seq("--input-file", "foo"))) should be(None)
    fromCommandLineArgs(InputOutputFileArgParser(Seq("--output-file", "foo"))) should be(None)
    
    val expected = InputOutputFileConfig("foo", "bar")
    
    fromCommandLineArgs(InputOutputFileArgParser(Seq("-i", "foo", "-o", "bar"))) should be(Some(expected))
    
    fromCommandLineArgs(InputOutputFileArgParser(Seq("--input-file", "foo", "--output-file", "bar"))) should be(Some(expected))
  }
}