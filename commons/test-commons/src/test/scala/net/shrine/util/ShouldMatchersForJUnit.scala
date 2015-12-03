package net.shrine.util

import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.Matchers

/**
 * @author clint
 * @date Jul 10, 2014
 * 
 * Aggreation trait to easily work around lots of deprecation warnings regarding
 * org.scalatest.junit.ShouldMatchersForJUnit.  Follows that class's advice and
 * extends org.scalatest.Matchers and org.scalatest.junit.AssertionsForJUnit.
 */
trait ShouldMatchersForJUnit extends Matchers with AssertionsForJUnit