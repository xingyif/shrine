package net.shrine.problem

import net.shrine.log.Loggable
import net.shrine.util.ShouldMatchersForJUnit
import org.apache.log4j.Level

import scala.collection.mutable.{Map => MMap}

/**
 *
 * @author dwalend
 * @since 1.20
 */
final class ProblemHandlerTest extends ShouldMatchersForJUnit {

  def testProblemHandler() = {

    val loggable = new MockLoggable

    val fakeException = new RuntimeException("test exception")
    fakeException.fillInStackTrace()

    val problem = TestProblem("testProblem","Problem created for testing",Some(fakeException))

    val problemHandler = LoggingProblemHandler

    problemHandler.handleProblem(problem)

    loggable.loggedAt(Level.ERROR) should be (right = true)

  }

  private final class MockLoggable extends Loggable {
    val loggedAt: MMap[Level, Boolean] = MMap.empty

    override def debug(s: => Any) {
      loggedAt(Level.DEBUG) = true
      
      super.debug(s)
    }

    override def info(s: => Any) {
      loggedAt(Level.INFO) = true
      
      super.info(s)
    }

    override def warn(s: => Any) {
      loggedAt(Level.WARN) = true

      super.warn(s)
    }

    override def error(s: => Any) {
      loggedAt(Level.ERROR) = true
      
      super.error(s)
    }
  }
}