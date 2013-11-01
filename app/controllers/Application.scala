package controllers

import play.api.mvc._
import java.util.UUID
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import model.command.{ExampleDomain, Person}
import model.query._
import scala.Some
import model.command.PersonCommands.CreatePerson
import model.common.Gender
import org.joda.time.DateTime

object Application extends Controller {

  implicit val timeout = Timeout(5 seconds)

  val addressBook = ExampleDomain.model.subscribe(Person, AddressBook.props)

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

  def create(firstName: String, lastName:String, gender:Gender, dob: DateTime) = Action {
    val id = UUID.randomUUID
    val person = ExampleDomain.model.aggregateRootOf(Person, id)
    person ! CreatePerson(firstName, lastName, gender, dob)
    Ok(Json.obj("id" -> id.toString))
  }
}