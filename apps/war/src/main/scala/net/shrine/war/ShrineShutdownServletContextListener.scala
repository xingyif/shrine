package net.shrine.war

import javax.servlet.{ServletContextEvent, ServletContextListener}

import net.shrine.log.Loggable

/**
 * @author clint
 * @since May 18, 2014
 * 
 * ServletContextListener to perform any Shrine-wide shutdown and startup tasks.
 * For now, just logs.
 */
final class ShrineShutdownServletContextListener extends ServletContextListener with Loggable {

  override def contextDestroyed(context: ServletContextEvent): Unit = info("Shrine node shut down")

  override def contextInitialized(context: ServletContextEvent): Unit = info("Shrine node initialized")
} 