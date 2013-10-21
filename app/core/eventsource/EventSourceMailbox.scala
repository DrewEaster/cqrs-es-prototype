package core.eventsource

import akka.dispatch._
import java.util.concurrent.LinkedBlockingDeque
import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.Config
import core.eventsource.EventSourceProtocol.AfterPersist

case class EventSourceMailbox() extends MailboxType with ProducesMessageQueue[EventSourceMailbox.MessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this()

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new EventSourceMailbox.MessageQueue
}

object EventSourceMailbox {

  class MessageQueue extends LinkedBlockingDeque[Envelope] with EventSourceMessageQueue {
    final val queue = this
  }
}

trait EventSourceMessageQueue extends UnboundedDequeBasedMessageQueue {
  override def enqueue(receiver: ActorRef, handle: Envelope) {
    handle.message match {
      case msg: AfterPersist => super.enqueueFirst(receiver, handle)
      case _ => super.enqueue(receiver, handle)
    }
  }
}
