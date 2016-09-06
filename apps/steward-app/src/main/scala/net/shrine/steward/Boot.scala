package net.shrine.steward

import akka.actor.{ActorSystem, Props}
import net.shrine.log.Loggable
import net.shrine.steward.db.StewardDatabase
import spray.servlet.WebBoot

// this class is instantiated by the servlet initializer
// it needs to have a default constructor and implement
// the spray.servlet.WebBoot trait
class Boot extends WebBoot with Loggable {

  info(s"StewardActors akka daemonic config is ${StewardConfigSource.config.getString("akka.daemonic")}")

  val warmUp = StewardDatabase.db.warmUp

  // we need an ActorSystem to host our application in
  val system = ActorSystem("StewardActors",StewardConfigSource.config)

  // the service actor replies to incoming HttpRequests
  val serviceActor = system.actorOf(Props[StewardServiceActor])

}