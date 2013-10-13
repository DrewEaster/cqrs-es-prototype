package core.eventsource

trait EntityBuilder[Entity] {
  def build: Entity
}