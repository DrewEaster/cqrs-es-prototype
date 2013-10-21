package core.eventsource

import akka.actor.ActorRef

case class EventSourceContext(commandBuffer: ActorRef, eventStore: EventStore, eventStream: ActorRef)
