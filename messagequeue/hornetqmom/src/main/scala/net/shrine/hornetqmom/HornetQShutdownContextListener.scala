package net.shrine.hornetqmom
import javax.servlet.{ServletContextEvent, ServletContextListener}

import akka.event.Logging
import spray.routing.directives.LogEntry

/**
  * Created by yifan on 8/31/17.
  */

class HornetQShutdownContextListener extends ServletContextListener {


  override def contextInitialized(servletContextEvent: ServletContextEvent): Unit = {
    LogEntry(s"${getClass.getSimpleName} context initialized $servletContextEvent", Logging.InfoLevel)

  }

  override def contextDestroyed(servletContextEvent: ServletContextEvent): Unit = {
    LocalHornetQMomStopper.stop()
    MessageScheduler.shutDown()
    LogEntry(s"${getClass.getSimpleName} context destroyed $servletContextEvent", Logging.InfoLevel)
  }
}