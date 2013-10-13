package core.eventsource

/**
  */
object EventSourceProtocol {

  case class RefreshState(events: Seq[Event])

  case class AppendState(events: Seq[Event])

  case object Replay

  case class Replay[T](entity: T, seqNum: Long)

  case class ErrorSavingState(events: Seq[Event], retryCount: Int, lastError: EventStoreError)

  case class ErrorLoadingState(retryCount: Int, lastError: EventStoreError)
}
