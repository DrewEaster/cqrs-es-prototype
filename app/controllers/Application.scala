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
import com.dreweaster.thespian.domain.DomainModel
import model.command.{AddressBookDomain, Person}
import model.query._
import scala.Some
import model.command.PersonCommands.CreatePerson

object Application extends Controller {

  implicit val timeout = Timeout(5 seconds)

  val addressBook = AddressBookDomain.model.subscribe(Person, AddressBook.props)

  def index = Action.async {
    (addressBook ? GetEntries).map {
      case values =>
        val entries = values.asInstanceOf[Seq[AddressBookEntry]]
        Ok(entries.foldLeft(Json.arr()) {
          (jsArray, entry) =>
            jsArray :+ Json.obj(
              "id" -> entry.id.toString,
              "first_name" -> entry.firstName,
              "last_name" -> entry.lastName,
              "gender" -> entry.gender,
              "version" -> entry.version)
        })
    }
  }

  def show(id: String) = Action.async {
    (addressBook ? GetEntry(UUID.fromString(id))).map {
      case Some(entry: AddressBookEntry) => Ok(
        Json.obj(
          "id" -> entry.id.toString,
          "first_name" -> entry.firstName,
          "last_name" -> entry.lastName,
          "gender" -> entry.gender,
          "version" -> entry.version))
      case None => NotFound
    }
  }

  def create(firstName: String, lastName:String, gender:String, dob: String) = Action {
    val id = UUID.randomUUID
    val person = AddressBookDomain.model.aggregateRootOf(Person, id)
    person ! CreatePerson
    Ok(Json.obj("id" -> id.toString))
  }
}