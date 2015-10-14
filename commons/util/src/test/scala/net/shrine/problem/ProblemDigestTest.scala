package net.shrine.problem

import net.shrine.util.ShouldMatchersForJUnit

/**
 *
 * @author dwalend
 * @since 1.20
 */
final class ProblemDigestTest extends ShouldMatchersForJUnit {

  def testRoundTrip() = {
    val problemDigest = ProblemDigest(getClass.getName,"stampText","Test problem","A problem for testing","We use this problem for testing. Don't worry about it")

    val xml = problemDigest.toXml
    val fromXml = ProblemDigest.fromXml(xml)

    fromXml should be(problemDigest)
  }
}

object TestProblem extends AbstractProblem(ProblemSources.Unknown) {
  override def summary: String = "Test problem summary"

  override def description: String = "Test problem description"
}
