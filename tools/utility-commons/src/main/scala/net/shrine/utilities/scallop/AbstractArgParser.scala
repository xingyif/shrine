package net.shrine.utilities.scallop

import org.rogach.scallop.ScallopConf
import net.shrine.util.Versions

/**
 * @author clint
 * @date Jul 21, 2014
 */
abstract class AbstractArgParser(override val args: Seq[String]) extends ScallopConf(args) {

  final val showVersionToggle = toggle("version")

  final val showHelpToggle = toggle("help")

  final def shouldShowVersion = showVersionToggle.isSupplied

  final def shouldShowHelp = showHelpToggle.isSupplied

  //NB: Prevents Scallop from calling System.exit on any errors; override as needed
  override protected def onError(e: Throwable): Unit = {
    if (!args.isEmpty && !shouldShowHelp && !shouldShowVersion) {
      System.err.println(e.getMessage)
    }
  }

  private def exitAfter(f: => Any): Unit = { f ; System.exit(0) }
  
  def showVersion(appName: String): Unit = println(Versions.versionString(appName))
  
  def showVersionAndExit(appName: String): Unit = exitAfter { showVersion(appName) }

  def showHelp(appName: String): Unit = { showVersion(appName) ; printHelp() }
  
  def showHelpAndExit(appName: String): Unit = exitAfter { showHelp(appName) }
}