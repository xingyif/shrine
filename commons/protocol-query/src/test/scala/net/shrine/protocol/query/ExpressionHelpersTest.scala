package net.shrine.protocol.query

import net.shrine.util.ShouldMatchersForJUnit
import org.junit.Test

/**
 * @author clint
 * @date Dec 17, 2012
 */
final class ExpressionHelpersTest extends ShouldMatchersForJUnit {
  @Test
  def testIs {
	import ExpressionHelpers.is
	
	is[Expression](And()) should equal(true)
	is[Expression](Term("foo")) should equal(true)
	
	is[ComposeableExpression[_]](Term("foo")) should be(false)
	
	is[And](And()) should equal(true)
	is[And](Or()) should equal(false)
	is[Or](And()) should equal(false)
	is[Or](Or()) should equal(true)
	
	is[String]("ksahdksa") should equal(true)
	is[Int]("sakljd") should equal(false)
  }
}