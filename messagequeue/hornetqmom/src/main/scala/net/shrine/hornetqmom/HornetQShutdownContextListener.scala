package net.shrine.hornetqmom
import javax.servlet.{ServletContextEvent, ServletContextListener}

import akka.event.Logging
import spray.routing.directives.LogEntry

/**
  * Created by yifan on 8/31/17.
  */

class HornetQShutdownContextListener extends ServletContextListener {


  override def contextInitialized(servletContextEvent: ServletContextEvent): Unit = {
    LogEntry(s"Tomcat context initialized for meta-app", Logging.InfoLevel)

  }

  override def contextDestroyed(servletContextEvent: ServletContextEvent): Unit = {
    LocalHornetQMomStopper.stop()
    LogEntry(s"Tomcat context destroyed and HornetQ server is stopped for meta-app", Logging.InfoLevel)
  }
}