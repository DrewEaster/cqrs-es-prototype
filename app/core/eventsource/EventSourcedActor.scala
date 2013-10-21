package core.eventsource

import java.util.UUID
import akka.actor._
import scala.collection.mutable.ListBuffer
import core.eventsource.EventSourceProtocol._
import core.eventsource.EventSourceProtocol.ErrorSavingState
import core.eventsource.EventSourceProtocol.RefreshState
import core.eventsource.EventSourceProtocol.AppendState
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import akka.dispatch.DequeBasedMessageQueueSemantics
import scala.collection.immutable.Queue
import core.eventsource.Event
import core.eventsource.EventSourceProtocol.ErrorSavingState
import core.eventsource.EventSourceProtocol.RefreshState
import core.eventsource.EventSourceProtocol.ErrorLoadingState
import core.eventsource.EventSourceProtocol.AppendState
import scala.None
import java.util.concurrent.{Executors, ConcurrentLinkedQueue}

abstract class EventSourcedActor[Entity, Builder <: EntityBuilder[Entity]](val id: UUID, val esContext: EventSourceContext) extends Actor {
  val maxRetryCount = 3

  private var cached = cache(newBuilder)

  private var currentSeqNum = 0L

  def entity = cached._2

  replay()

  def receive = {
    case AppendState(events) => {
      cached = cache(cached._1, events)
      publish(events)
      esContext.commandBuffer ! Ack
    }
    case RefreshState(events) => {
      cached = cache(newBuilder, events)
      esContext.commandBuffer ! Ack
    }
    case ErrorSavingState(events, retryCount, lastError) => {
      if (retryCount >= maxRetryCount) context.become(unstable)
      else save(events, retryCount + 1)
    }
    case ErrorLoadingState(retryCount, lastError) => {
      if (retryCount >= maxRetryCount) context.become(unstable)
      else replay(retryCount + 1)
    }
    case msg => handleCommand(msg)
  }

  protected def unstable: Receive = {
    throw new IllegalStateException()
  }

  protected def handleCommand(cmd: Any)

  protected def cache(state: Builder, events: Seq[Event] = Seq()) = {
    val update = events.foldLeft((0L, state))((acc, e) => (e.seqNum, handleEvent(acc._2, e.data)))
    currentSeqNum = update._1
    (update._2, update._2.build)
  }

  protected def newBuilder: Builder

  protected def handleEvent(builder: Builder, event: EventData): Builder

  protected def unitOfWork(work: ListBuffer[EventData] => Unit) {
    val buffer = new ListBuffer[EventData]
    work(buffer)
    save(buffer.reverse.foldLeft((currentSeqNum, List[Event]()))((acc, data) => (acc._1 + 1, Event(data, acc._1 + 1) :: acc._2))._2)
  }

  private def publish(events: Seq[Event]) {
    for (e <- events) esContext.eventStream ! e
  }

  private def replay(retryCount: Int = 0) {
    esContext.eventStore.loadEvents.map {
      case Right(events) => self ! RefreshState(events)
      case Left(ex) => self ! ErrorLoadingState(retryCount, ex)
    }
  }

  private def save(events: Seq[Event], retryCount: Int = 0) {
    esContext.eventStore.saveEvents(events: _*).map {
      case Right(e) => self ! AppendState(e)
      case Left(ex) => self ! ErrorSavingState(events, retryCount, ex)
    }
  }
}

case object Ack

class AggregateRoot(eventStream: ActorRef, processorFactory: EventSourceContext => ActorRef) extends Actor {

  val eventStore = new InMemoryEventStore()

  val commandBuffer = context.system.actorOf(Props(new CommandBufferActor()))

  val processor = processorFactory(EventSourceContext(commandBuffer, eventStore, eventStream))

  def receive = {
    case msg => commandBuffer forward msg
  }
}

class CommandBufferActor extends Actor {

  var buffer = new ConcurrentLinkedQueue[Any]()

  var destination: Option[ActorRef] = None

  def receive = {
    case Ack => {
      val msg = buffer.poll
      if (msg != null) {
        sender ! msg
        destination = None
      } else {
        destination = Some(sender)
      }
    }
    case msg => {
      buffer.add(msg)
      destination match {
        case Some(sender) => {
          sender ! buffer.poll
          destination = None
        }
        case None => destination = None
      }
    }
  }
}