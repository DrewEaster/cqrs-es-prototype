package model

import java.util.UUID
import core.eventsource._
import akka.actor.{ActorLogging, ActorRef}
import model.CustomerBuilder
import model.ChangeCustomerAge
import core.eventsource.Event
import model.CustomerCreated
import model.CustomerAgeChanged
import model.ChangeCustomerName
import model.Customer
import model.CustomerNameChanged
import model.CreateCustomer

// Commands
case class CreateCustomer(customerId: UUID, name: String, age: Int)

case class ChangeCustomerName(customerId: UUID, name: String)

case class ChangeCustomerAge(customerId: UUID, age: Int)

// Events
case class CustomerNameChanged(customerId: UUID, name: String) extends EventData

case class CustomerAgeChanged(customerId: UUID, age: Int) extends EventData

case class CustomerCreated(customerId: UUID, name: String, age: Int) extends EventData

// Internal state
case class Customer(customerId: UUID, age: Int, name: String)

// Builder
case class CustomerBuilder(customerId: UUID, age: Int, name: String) extends EntityBuilder[Customer] {
  def build = Customer(customerId, age, name)
}

class CustomerAR(id: UUID, eventStore: EventStore, eventStream: ActorRef)
  extends EventSourcedActor[Customer, CustomerBuilder](id, eventStore, eventStream) with ActorLogging {

  def newBuilder = CustomerBuilder(id, 0, "")

  def handleEvent(builder: CustomerBuilder, event: EventData): CustomerBuilder = event match {
    case CustomerCreated(_, name, age) => builder.copy(name = name, age = age)
    case CustomerNameChanged(_, newName) => builder.copy(name = newName)
    case CustomerAgeChanged(_, newAge) => builder.copy(age = newAge)
  }

  def handleCommand = {
    case CreateCustomer(customerId, name, age) =>
      unitOfWork {
        log.info("Creating customer...")
        _ += CustomerCreated(customerId, name, age)
      }
    case ChangeCustomerName(customerId, newName) =>
      unitOfWork {
        log.info("Changing customer name...")
        _ += CustomerNameChanged(customerId, newName)
      }
    case ChangeCustomerAge(customerId, newAge) =>
      unitOfWork {
        log.info("Changing customer age...")
        _ += CustomerAgeChanged(customerId, newAge)
      }
  }
}