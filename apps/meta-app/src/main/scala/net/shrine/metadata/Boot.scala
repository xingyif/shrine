package net.shrine.metadata

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.routing.RoundRobinPool
import net.shrine.problem.ProblemHandler
import net.shrine.source.ConfigSource
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

  def startActorSystem() = try ActorSystem("ShrineActors",ConfigSource.config)
  catch {
    case NonFatal(x) => CannotStartMetaData(x); throw x
    case x: ExceptionInInitializerError => CannotStartMetaData(x); throw x
  }

  def startServiceActor(): ActorRef = try {
    //TODO: create a common interface for Problems to hide behind, so that it doesn't exist anywhere in the code
    //TODO: except for when brought into scope
    val handler:ProblemHandler = ConfigSource.getObject("shrine.problem.problemHandler", ConfigSource.config)
    handler.warmUp()

    // the service actor replies to incoming HttpRequests
    system.actorOf(RoundRobinPool(100).props(Props[MetaDataActor]), "MetaDataActors") //todo what is a reasonable number? (I've asked the google group 8-31)
  }
  catch {
    case NonFatal(x) => CannotStartMetaData(x); throw x
    case x: ExceptionInInitializerError => CannotStartMetaData(x); throw x
  }

}
