package model.command

import org.joda.time.DateTime
import model.common.Gender

object PersonCommands {

  case class CreatePerson(firstName: String, lastName: String, gender: Gender, dob: DateTime)

}
