package net.shrine.log

import net.shrine.util.ShouldMatchersForJUnit
import org.apache.log4j.Level

import scala.collection.mutable.{Map => MMap}

/**
 *
 * @author Clint Gilbert
 * @date Oct 11, 2011
 *
 * @link http://cbmi.med.harvard.edu
 *
 * This software is licensed under the LGPL
 * @link http://www.gnu.org/licenses/lgpl.html
 *
 */
final class LoggableTest extends ShouldMatchersForJUnit {

  import Level.{DEBUG, ERROR, FATAL, INFO, WARN}

  def testDebug = doTest(_.debug, DEBUG, logMessageIsLazy = true)

  def testInfo = doTest(_.info, INFO, logMessageIsLazy = true)

  def testWarn = doTest(_.warn, WARN, logMessageIsLazy = false)

  def testError = doTest(_.error, ERROR, logMessageIsLazy = false)

  private def doTest(log: Loggable => (=> Any) => Unit, level: Level, logMessageIsLazy: Boolean) {
    {
      val loggable = new MockLoggable

      var messageComputed = false

      val otherLevel = if (logMessageIsLazy) higher(level) else not(level)

      loggable.logger.setLevel(otherLevel)

      log(loggable)({ messageComputed = true; "message" })

      {
        val shouldHaveBeenComputed = !logMessageIsLazy

        //the log message should NOT have been computed if this log method is lazy
        messageComputed should be(shouldHaveBeenComputed)
        //but we should have tried to log
        loggable.loggedAt(level) should be(true)
      }
    }

    {
      val loggable = new MockLoggable

      var messageComputed = false

      loggable.logger.setLevel(level)

      log(loggable)({ messageComputed = true; "message" })

      //the log message should have been computed 
      messageComputed should be(true)
      //we should have tried to log at the desired level
      loggable.loggedAt(level) should be(true)
      //and at no other levels
      loggable.loggedAt.exists { case (priority, happened) => priority != priority && happened == true } should be(false)
    }
  }

  private val priorities: Map[Level, Int] = Seq(DEBUG, INFO, WARN, ERROR, FATAL).zipWithIndex.toMap

  private def next(level: Level, adjust: Int => Int): Level = {
    val numericalPriority = priorities(level)
    
    val adjustedPriority = adjust(numericalPriority)

    priorities.find { case (_, index) => index == adjustedPriority }.map { case (p, _) => p }.get
  }

  private def not(level: Level): Level = priorities.keys.find(_ != level).get
  private def higher(level: Level): Level = next(level, _ + 1)
  private def lower(level: Level): Level = next(level, _ - 1)

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