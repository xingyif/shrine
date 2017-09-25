package net.shrine.dashboard

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import net.shrine.problem.{AbstractProblem, ProblemHandler, ProblemSources}
import net.shrine.source.ConfigSource
import spray.servlet.WebBoot

import scala.util.control.NonFatal

// this class is instantiated by the servlet initializer
// it needs to have a default constructor and implement
// the spray.servlet.WebBoot trait
class Boot extends WebBoot {

  // we need an ActorSystem to host our application in
  override val system = startActorSystem()

  // the service actor replies to incoming HttpRequests
  override val serviceActor: ActorRef = startServiceActor()

  def startActorSystem() = try ActorSystem("DashboardActors",ConfigSource.config)
  catch {
    case NonFatal(x) => CannotStartDashboard(x); throw x
    case x: ExceptionInInitializerError => CannotStartDashboard(x); throw x
  }

  def startServiceActor() = try {
    //TODO: create a common interface for Problems to hide behind, so that it doesn't exist anywhere in the code
    //TODO: except for when brought into scope by a DatabaseProblemHandler
    val handler:ProblemHandler = ConfigSource.getObject("shrine.problem.problemHandler", ConfigSource.config)
    handler.warmUp()

    // the service actors reply to incoming HttpRequests
    system.actorOf(RoundRobinPool(100).props(Props[DashboardServiceActor]), "DashboardServiceActors")
  }
  catch {
    case NonFatal(x) => CannotStartDashboard(x); throw x
    case x: ExceptionInInitializerError => CannotStartDashboard(x); throw x
  }
}

case class CannotStartDashboard(ex:Throwable) extends AbstractProblem(ProblemSources.Dsa) {
  override def summary: String = "The Dashboard could not start due to an exception."

  override def description: String = s"The Dashboard could not start due to ${throwable.get}"

  override def throwable = Some(ex)
}