package net.shrine.dashboard

import akka.actor.{ActorSystem, Props}
import net.shrine.problem.Problems
import spray.servlet.WebBoot

// this class is instantiated by the servlet initializer
// it needs to have a default constructor and implement
// the spray.servlet.WebBoot trait
class Boot extends WebBoot {

  val warmUp:Unit = Problems.warmUp()

  // we need an ActorSystem to host our application in
  val system = ActorSystem("DashboardActors",DashboardConfigSource.config)

  // the service actor replies to incoming HttpRequests
  val serviceActor = system.actorOf(Props[DashboardServiceActor])

}