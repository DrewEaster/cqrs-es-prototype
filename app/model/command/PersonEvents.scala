package model.command

import org.joda.time.DateTime
import model.common.Gender

object PersonEvents {

  case class PersonCreated(firstName: String, lastName: String, gender: Gender, dob: DateTime)

}
