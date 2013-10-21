package core.eventsource

/**
  */
object EventSourceProtocol {

  trait AfterPersist

  case class RefreshState(events: Seq[Event]) extends AfterPersist

  case class AppendState(events: Seq[Event]) extends AfterPersist

  case object Replay

  case class Replay[T](entity: T, seqNum: Long)

  case class ErrorSavingState(events: Seq[Event], retryCount: Int, lastError: EventStoreError) extends AfterPersist

  case class ErrorLoadingState(retryCount: Int, lastError: EventStoreError) extends AfterPersist
}
