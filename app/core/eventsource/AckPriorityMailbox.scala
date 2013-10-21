package core.eventsource

import akka.dispatch._
import java.util.concurrent.LinkedBlockingDeque
import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.Config
import core.eventsource.EventSourceProtocol.AfterPersist

case class AckPriorityMailbox() extends MailboxType with ProducesMessageQueue[AckPriorityMailbox.MessageQueue] {

  def this(settings: ActorSystem.Settings, config: Config) = this()

  final override def create(owner: Option[ActorRef], system: Option[ActorSystem]): MessageQueue =
    new AckPriorityMailbox.MessageQueue
}

object AckPriorityMailbox {

  class MessageQueue extends LinkedBlockingDeque[Envelope] with AckPriorityMessageQueue {
    final val queue = this
  }
}

trait AckPriorityMessageQueue extends UnboundedDequeBasedMessageQueue {
  override def enqueue(receiver: ActorRef, handle: Envelope) {
    handle.message match {
      case Ack => super.enqueueFirst(receiver, handle)
      case msg => super.enqueue(receiver, handle)
    }
  }
}
