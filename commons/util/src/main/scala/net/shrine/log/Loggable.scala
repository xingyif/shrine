package net.shrine.log

import org.apache.log4j.Logger

/**
 * Apparently this is how the scala community likes to boilerplate their loggers
 *
 * @author Justin Quan
 * @see http://chip.org
 * Date: 8/8/11
 */
trait Loggable {
  private[this] lazy val internalLogger: Logger = Logger.getLogger(this.getClass.getName)
  
  //NB: For tests
  private[log] def logger: Logger = internalLogger
  
  //NB: This used to be a lazy val, but that was confusing Squeryl, so this is now a def
  final def debugEnabled = internalLogger.isDebugEnabled
  
  //NB: This used to be a lazy val, but that was confusing Squeryl, so this is now a def
  final def infoEnabled = internalLogger.isInfoEnabled
  
  def debug(s: => Any): Unit = if(debugEnabled) internalLogger.debug(s)
  final def debug(s: => Any, e: Throwable): Unit = if(debugEnabled) internalLogger.debug(s, e)
  
  def info(s: => Any): Unit = if(infoEnabled) internalLogger.info(s)
  final def info(s: => Any, e: Throwable): Unit = if(infoEnabled) internalLogger.info(s, e)
  
  def warn(s: => Any): Unit = internalLogger.warn(s)
  final def warn(s: => Any, e: Throwable): Unit = internalLogger.warn(s, e)
  
  def error(s: => Any): Unit = internalLogger.error(s)
  final def error(s: => Any, e: Throwable): Unit = internalLogger.error(s, e)
}

/**
 * Simple Log object for when Loggable isn't available.
 *
 * @author dwalend
 * @since 7/20/2015
 */
object Log extends Loggable


