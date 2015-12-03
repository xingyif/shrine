package net.shrine.utilities.commands

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Mar 25, 2013
 */
final class CompoundCommandTest extends ShouldMatchersForJUnit {

  @Test
  def testApply {
    val longToInt: MockCommand[Long, Int] = MockCommand(_.toInt)

    val intToString: MockCommand[Int, String] = MockCommand(_.toString)

    val longToIntToString: Long >>> String = CompoundCommand(longToInt, intToString)

    val s = longToIntToString(123L)

    s should equal("123")

    longToInt.invoked should be(true)

    intToString.invoked should be(true)
  }
}