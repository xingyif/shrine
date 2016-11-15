package net.shrine.dashboard

import akka.actor.{ActorRef, ActorSystem, Props}
import net.shrine.log.Log
import net.shrine.problem.{AbstractProblem, ProblemConfigSource, ProblemHandler, ProblemSources}
import spray.servlet.WebBoot

import scala.util.control.NonFatal

// this class is instantiated by the servlet initializer
// it needs to have a default constructor and implement
// the spray.servlet.WebBoot trait
class Boot extends WebBoot {

  println("Start of dashboard boot")
  Log.info("Start of dashboard boot")

  // we need an ActorSystem to host our application in
  override val system = startActorSystem()

  // the service actor replies to incoming HttpRequests
  override val serviceActor: ActorRef = startServiceActor()

  def startActorSystem() = try ActorSystem("DashboardActors",DashboardConfigSource.config)
  catch {
    case NonFatal(x) => CannotStartDashboard(x); throw x
    case x: ExceptionInInitializerError => CannotStartDashboard(x); throw x
  }

  def startServiceActor() = try {
    //TODO: create a common interface for Problems to hide behind, so that it doesn't exist anywhere in the code
    //TODO: except for when brought into scope by a DatabaseProblemHandler
    val handler:ProblemHandler = ProblemConfigSource.getObject("shrine.problem.problemHandler", ProblemConfigSource.config)
    handler.warmUp()

    // the service actor replies to incoming HttpRequests
    system.actorOf(Props[DashboardServiceActor])
  }
  catch {
    case NonFatal(x) => CannotStartDashboard(x); throw x
    case x: ExceptionInInitializerError => CannotStartDashboard(x); throw x
  }

  println("End of dashboard boot")
  Log.info("End of dashboard boot")

}

case class CannotStartDashboard(ex:Throwable) extends AbstractProblem(ProblemSources.Dsa) {
  override def summary: String = "The Dashboard could not start due to an exception."

  override def description: String = s"The Dashboard could not start due to ${throwable.get}"

  override def throwable = Some(ex)
}