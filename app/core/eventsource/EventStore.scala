package core.eventsource

import scala.concurrent.Future

case class EventStoreError(ex: Throwable)

trait EventStore {

  def saveEvents(events: Event*): Future[Either[EventStoreError, Seq[Event]]]

  def loadEvents: Future[Either[EventStoreError, Seq[Event]]]

  def loadEvents(fromSeqNum: Long): Future[Either[EventStoreError, Seq[Event]]]

  //def loadSnapshot: Future[Option[Snapshot]]

  //def saveSnapshot(data: Any, seqNum: Long): Future[Either[EventStoreError, Snapshot]]
}
