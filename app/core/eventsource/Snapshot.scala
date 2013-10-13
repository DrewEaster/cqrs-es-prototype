package core.eventsource

case class Snapshot(data: Any, seqNum: Long, timestamp: Long)