package metadata

import akka.actor.{Actor, ActorRefFactory}

/**
  * A super simple API that provides access to the MetaData section of SHRINE's configuration
  */

class MetaDataActor extends Actor with MetaDataService {
  override def receive: Receive = runRoute(route)

  override def actorRefFactory: ActorRefFactory = context
}
