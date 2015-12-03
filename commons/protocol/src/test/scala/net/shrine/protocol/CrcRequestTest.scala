package net.shrine.protocol

import net.shrine.util.ShouldMatchersForJUnit
import junit.framework.TestCase
import org.junit.Test

/**
 * @author clint
 * @date Mar 29, 2013
 */
final class CrcRequestTest extends TestCase with ShouldMatchersForJUnit {
  @Test
  def testFromI2b2BadInput {
    CrcRequest.fromI2b2String(Set.empty)("jksahdjkashdjkashdjkashdjksad").isFailure should be(true)

    CrcRequest.fromI2b2(Set.empty)(<foo/>).isFailure should be(true)
  }
}