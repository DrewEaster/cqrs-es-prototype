package controllers

import play.api.mvc._
import java.util.UUID
import akka.actor._
import core.eventsource._
import play.libs.Akka
import model._
import akka.pattern.ask
import model.CustomerAgeChanged
import model.CustomerNameChanged
import scala.Some
import model.CreateCustomer
import akka.routing.Listen
import play.api.Logger
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import model.ChangeCustomerAge
import controllers.CustomerDTO
import model.CustomerCreated
import model.CustomerAgeChanged
import controllers.GetCustomer
import scala.Some
import akka.routing.Listen
import model.ChangeCustomerName
import model.CustomerNameChanged
import model.CreateCustomer
import model.ChangeCustomerAge
import controllers.CustomerDTO
import model.CustomerCreated
import model.CustomerAgeChanged
import controllers.GetCustomer
import scala.Some
import akka.routing.Listen
import core.eventsource.Event
import model.ChangeCustomerName
import model.CustomerNameChanged
import model.CreateCustomer

case class CustomerDTO(uuid: UUID, name: String, age: Int, version: Long)

case class GetCustomer(uuid: UUID)

case object GetCustomers

class CustomerModel extends Actor with ActorLogging {

  val model = scala.collection.mutable.Map[UUID, CustomerDTO]()

  def receive = {
    case GetCustomers => {
      sender ! model.values.toSeq
    }
    case GetCustomer(id) => {
      val customer = model(id)
      if (customer == null) sender ! None else sender ! Some(customer)
    }
    case Event(CustomerCreated(id, name, age), version) => {
      model.put(id, CustomerDTO(id, name, age, version))
    }
    case Event(CustomerNameChanged(id, name), version) => {
      val customer = model(id)
      if (customer != null) model.put(id, customer.copy(name = name, version = version))
      else Logger.warn("Customer " + id + " not in read model")
    }
    case Event(CustomerAgeChanged(id, age), version) => {
      val customer = model(id)
      if (customer != null) model.put(id, customer.copy(age = age, version = version))
      else Logger.warn("Customer " + id + " not in read model")
    }
  }
}

object Application extends Controller {

  implicit val timeout = Timeout(5 seconds)

  val readModel = Akka.system.actorOf(Props[CustomerModel], "customerModel")

  val eventStream = Akka.system.actorOf(Props[EventStreamActor], "eventStream")

  // Not thread safe!
  val customers = scala.collection.mutable.Map[UUID, ActorRef]()

  eventStream ! Listen(readModel)

  def index = Action.async {
    (readModel ? GetCustomers).map {
      case values =>
        val customers = values.asInstanceOf[Seq[CustomerDTO]]
        Ok(customers.foldLeft(Json.arr()) {
          (jsArray, customer) =>
            jsArray :+ Json.obj(
              "id" -> customer.uuid.toString,
              "name" -> customer.name,
              "age" -> customer.age,
              "version" -> customer.version)
        })
    }
  }

  def show(id: String) = Action.async {
    (readModel ? GetCustomer(UUID.fromString(id))).map {
      case Some(customer: CustomerDTO) => Ok(
        Json.obj(
          "id" -> customer.uuid.toString,
          "name" -> customer.name,
          "age" -> customer.age,
          "version" -> customer.version))
      case None => NotFound
    }
  }

  def create(name: String, age: Int) = Action {
    val uuid = UUID.randomUUID
    val customerActor = Akka.system.actorOf(Props(new AggregateRoot(eventStream,
      context => Akka.system.actorOf(Props(new CustomerAR(uuid, context)), name = "customer:" + uuid)
    )))
    customers.put(uuid, customerActor)
    customerActor ! CreateCustomer(uuid, name, age)
    Ok(UUID.randomUUID.toString).as(JSON)
  }

  def changeName(id: String, newName: String) = Action {
    val customer = customers(UUID.fromString(id))
    if (customer != null) {
      customer ! ChangeCustomerName(UUID.fromString(id), newName)
      Ok
    } else NotFound
  }

  def changeAge(id: String, newAge: Int) = Action {
    val customer = customers(UUID.fromString(id))
    if (customer != null) {
      customer ! ChangeCustomerAge(UUID.fromString(id), newAge)
      Ok
    } else NotFound
  }
}