package net.shrine.war

import javax.servlet.ServletContextListener
import javax.servlet.ServletContextEvent
import net.shrine.log.Loggable
import net.shrine.wiring.ShrineConfig

/**
 * @author clint
 * @date May 18, 2014
 * 
 * ServletContextListener to perform any Shrine-wide shutdown and startup tasks.
 * For now, just logs.
 */
final class ShrineShutdownServletContextListener extends ServletContextListener with Loggable {

  override def contextDestroyed(context: ServletContextEvent): Unit = info("Shrine node shut down")

  override def contextInitialized(context: ServletContextEvent): Unit = info("Shrine node initialized")
} 