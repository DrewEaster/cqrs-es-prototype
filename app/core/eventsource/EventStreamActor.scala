package core.eventsource

import akka.actor.Actor
import akka.routing.Listeners

/**
 * EventStreamActor receives events and publishes it to it's listeners
 */
class EventStreamActor extends Actor with Listeners {

  def receive = listenerManagement orElse {
    case e: Event => gossip(e)
  }
}
