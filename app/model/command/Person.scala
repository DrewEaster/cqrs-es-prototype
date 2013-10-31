package model.command

import com.dreweaster.thespian.domain.{AggregateRootType, AggregateRoot}
import org.joda.time.DateTime
import model.command.PersonEvents.PersonCreated
import model.common.{Male, Gender}
import model.command.PersonCommands.CreatePerson

object Person extends AggregateRootType {
  val typeInfo = classOf[Person]
}

case class PersonState(firstName: String = "", lastName: String = "", gender: Gender = Male, dob: DateTime = new DateTime()) {
  def update: Any => PersonState = {
    case PersonCreated(fn, ln, g, d) => copy(firstName = fn, lastName = ln, gender = g, dob = d)
  }
}

class Person extends AggregateRoot {
  var state = PersonState()

  def fetchState = state

  def applyState = e => state = state.update(e)

  def loadState = s => state = s.asInstanceOf[PersonState]

  def handleCommand: Receive = {
    case CreatePerson(name, age, gender, dob) => unitOfWork {
      PersonCreated(name, age, gender, dob)
    }
  }
}
