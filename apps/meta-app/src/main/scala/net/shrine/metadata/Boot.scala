package net.shrine.metadata

import akka.actor.{ActorRef, ActorSystem, Props}
import net.shrine.problem.{ProblemConfigSource, ProblemHandler}
import spray.servlet.WebBoot

import scala.util.control.NonFatal

/**
  * Created by ty on 11/8/16.
  */
// this class is instantiated by the servlet initializer
// it needs to have a default constructor and implement
// the spray.servlet.WebBoot trait
class Boot extends WebBoot {

  // we need an ActorSystem to host our application in
  override val system = startActorSystem()

  // the service actor replies to incoming HttpRequests
  override val serviceActor: ActorRef = startServiceActor()

  def startActorSystem() = try ActorSystem("MetaDataActors",MetaConfigSource.config)
  catch {
    case NonFatal(x) => CannotStartMetaData(x); throw x
    case x: ExceptionInInitializerError => CannotStartMetaData(x); throw x
  }

  def startServiceActor() = try {
    //TODO: create a common interface for Problems to hide behind, so that it doesn't exist anywhere in the code
    //TODO: except for when brought into scope by a DatabaseProblemHandler
    val handler:ProblemHandler = ProblemConfigSource.getObject("shrine.problem.problemHandler", ProblemConfigSource.config)
    handler.warmUp()

    // the service actor replies to incoming HttpRequests
    system.actorOf(Props[MetaDataActor])
  }
  catch {
    case NonFatal(x) => CannotStartMetaData(x); throw x
    case x: ExceptionInInitializerError => CannotStartMetaData(x); throw x
  }

}
