package core.eventsource

import scala.concurrent._
import ExecutionContext.Implicits.global
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * An in memory implementation of {@link EventStorage}
 */
class InMemoryEventStore extends EventStore {

  private val eventHistory = ListBuffer[Event]()

  /**
   * Saves the events in the order they occurred
   */
  def saveEvents(events: Event*) = {
    events.foreach { eventHistory += _}
    future {
      Right(events)
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