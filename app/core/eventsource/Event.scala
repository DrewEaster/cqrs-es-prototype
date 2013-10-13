package core.eventsource

trait EventData

case class Event(data: EventData, seqNum: Long = 0)