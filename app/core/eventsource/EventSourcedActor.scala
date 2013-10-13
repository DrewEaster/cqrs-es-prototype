package core.eventsource

import java.util.UUID
import akka.actor.{ActorInitializationException, Stash, Actor, ActorRef}
import scala.collection.mutable.ListBuffer
import core.eventsource.EventSourceProtocol._
import core.eventsource.EventSourceProtocol.ErrorSavingState
import core.eventsource.EventSourceProtocol.RefreshState
import core.eventsource.EventSourceProtocol.AppendState
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import akka.dispatch.DequeBasedMessageQueueSemantics

abstract class EventSourcedActor[Entity, Builder <: EntityBuilder[Entity]](val id: UUID, val eventStore: EventStore, val eventStream: ActorRef) extends Actor with Stash {

  val maxRetryCount = 3

  private var cached = cache(newBuilder)

  private var currentSeqNum = 0L

  replay()

  def entity = cached._2

  def receive = busy

  protected def ready: Receive = {
    case Replay => replay()
    case _ => handleCommand
  }

  protected def busy: Receive = {
    case AppendState(events) => {
      context.become(handleCommand)
      unstashAll()
      cached = cache(cached._1, events)
      publish(events)
    }
    case RefreshState(events) => {
      context.become(handleCommand)
      unstashAll()
      cached = cache(newBuilder, events)
    }
    case ErrorSavingState(events, retryCount, lastError) => {
      if (retryCount >= maxRetryCount) context.become(unstable)
      else save(events, retryCount + 1)
    }
    case ErrorLoadingState(retryCount, lastError) => {
      if (retryCount >= maxRetryCount) context.become(unstable)
      else replay(retryCount + 1)
    }
    case _ => stash()
  }

  protected def unstable: Receive = {
    throw new IllegalStateException()
  }

  protected def handleCommand: Receive

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
    for (e <- events) eventStream ! e
  }

  private def replay(retryCount: Int = 0) {
    context.become(busy)
    eventStore.loadEvents.map {
      case Right(events) => self ! RefreshState(events)
      case Left(ex) => self ! ErrorLoadingState(retryCount, ex)
    }
  }

  private def save(events: Seq[Event], retryCount: Int = 0) {
    context.become(busy)
    eventStore.saveEvents(events: _*).map {
      case Right(e) => self ! AppendState(e)
      case Left(ex) => self ! ErrorSavingState(events, retryCount, ex)
    }
  }
}