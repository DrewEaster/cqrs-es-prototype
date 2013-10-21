package core.eventsource

import akka.actor.{ActorRef, Actor, Props, ActorSystem}
import model.{CustomerNameChanged, ChangeCustomerName, CreateCustomer, CustomerAR}
import java.util.UUID
import java.util.concurrent.{TimeUnit, CountDownLatch}
import akka.routing.Listen

/**
  */
object EventSourceTest extends App {

  val system = ActorSystem("cqrs-es-system")

  val eventStream = system.actorOf(Props[EventStreamActor], "eventStream")

  val customerId = UUID.randomUUID

  val customerActor = system.actorOf(Props(new AggregateRoot(eventStream,
    context => system.actorOf(Props(new CustomerAR(customerId, context)), name = "customer:" + customerId)
  )))

  val numberOfActors = 1

  val repeat = 50000

  val latch = new CountDownLatch(numberOfActors * repeat)

  val eventReceiver = system.actorOf(Props(new EventReceiver(latch)))

  eventStream ! Listen(eventReceiver)

  customerActor ! CreateCustomer(customerId, "andrew", 33)

  execute()

  def execute() {

    val clients = for {
      i <- 0 until numberOfActors
      actor = system.actorOf(Props(new NameChangeActor(customerActor, customerId, repeat)))
    } yield actor

    val start = System.nanoTime

    clients.foreach(_ ! "doStuff")

    val ok = latch.await(1000 * 60, TimeUnit.MILLISECONDS)

    val durationSeconds = (System.nanoTime - start) / 1000000000

    System.out.println("Total ms: " + ((System.nanoTime - start) / 1000000))

    if (ok) {
      val tps = (numberOfActors * repeat) / durationSeconds
      System.out.println("Success! Achieved " + tps + "tps")
    } else {
      System.out.println("Failed to complete test within time limit!")
    }

    clients.foreach(system.stop(_))
    system.stop(eventReceiver)
    system.stop(customerActor)

    system.shutdown
  }
}

class EventReceiver(val latch: CountDownLatch) extends Actor {
  var name = ""
  var count = 0

  def receive = {
    case Event(CustomerNameChanged(_, customerName), _) => name = customerName; count = count + 1; latch.countDown
  }

  override def postStop() {
    System.out.println("Total events: " + count)
    System.out.println("Current name: " + name)
    super.postStop()
  }
}

class NameChangeActor(val customerActor: ActorRef, val customerId: UUID, val count: Int) extends Actor {

  def receive = {
    case _ => for (i <- 0 to count) customerActor ! ChangeCustomerName(customerId = customerId, name = "andy" + i)
  }
}
