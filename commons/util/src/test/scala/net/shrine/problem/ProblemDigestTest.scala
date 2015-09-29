package net.shrine.problem

import net.shrine.util.ShouldMatchersForJUnit

/**
 *
 * @author dwalend
 * @since 1.20
 */
final class ProblemDigestTest extends ShouldMatchersForJUnit {

  def testRoundTrip() = {
    val problemDigest = ProblemDigest(getClass.getName,"Test problem","A problem for testing","We use this problem for testing. Don't worry about it")

    val xml = problemDigest.toXml
    val fromXml = ProblemDigest.fromXml(xml)

    fromXml should be(problemDigest)
  }
}