package net.shrine.utilities.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Mar 25, 2013
 */
final class CommandTest extends ShouldMatchersForJUnit {
  @Test
  def testAndThen {
    val f: Int >>> String = MockCommand(_.toString)
    val g: String >>> Unit = MockCommand(println)
    
    val CompoundCommand(actualF, actualG) = f andThen g
    
    actualF should be(f)
    actualG should be(g)
  }
}