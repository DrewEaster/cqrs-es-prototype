package core.eventsource

import java.util.UUID

/**
 * A factory for {@EventStorage} instances
 */
trait EventStoreFactory {
  def eventStorage(aggregateRootId: UUID)
}
