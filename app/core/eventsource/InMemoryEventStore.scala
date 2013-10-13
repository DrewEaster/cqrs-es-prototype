package core.eventsource

import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * An in memory implementation of {@link EventStorage}
 */
class InMemoryEventStore extends EventStore {

  private var eventHistory: List[Event] = Nil

  /**
   * Saves the events in the order they occurred
   */
  def saveEvents(events: Event*) = {
    eventHistory = eventHistory ::: events.toList
    future {
      Right(eventHistory)
    }
  }

  /**
   * Loads the events in the order they occurred
   */
  def loadEvents() = {
    future {
      Right(eventHistory)
    }
  }

  def loadEvents(fromSeqNum: Long): Future[Either[EventStoreError, Seq[Event]]] = {
    future {
      Right(eventHistory.filter(_.seqNum >= fromSeqNum))
    }
  }
}