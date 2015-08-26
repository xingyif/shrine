package net.shrine.war

import javax.servlet.ServletContextListener
import javax.servlet.ServletContextEvent
import net.shrine.adapter.AdapterMap

/**
 * @author clint
 * @date Dec 18, 2013
 */
final class AdapterMapShutdownServletContextListener extends ServletContextListener {
  override def contextDestroyed(context: ServletContextEvent) {
    AdapterMap.shutdownAllMaps()
  }

  override def contextInitialized(context: ServletContextEvent) {
    // NOOP
  }
} 