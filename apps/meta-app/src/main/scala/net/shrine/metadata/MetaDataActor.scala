package net.shrine.metadata

import akka.actor.{Actor, ActorRefFactory, ActorSystem}

import scala.concurrent.ExecutionContext

/**
  * A super simple API that provides access to the MetaData section of SHRINE's configuration
  */

class MetaDataActor extends Actor with MetaDataService {
  override def receive: Receive = runRoute(route)

  override def actorRefFactory: ActorRefFactory = context

  override def system: ActorSystem = context.system

  override implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
}