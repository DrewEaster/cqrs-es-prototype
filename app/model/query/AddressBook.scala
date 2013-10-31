package model.query

import java.util.UUID
import akka.actor.{Props, Actor}
import com.dreweaster.thespian.domain.Event
import com.dreweaster.thespian.example.model.command.CustomerEvents.{CustomerAgeChanged, CustomerNameChanged, CustomerCreated}
import model.common.Gender
import org.joda.time.DateTime
import model.command.PersonEvents.PersonCreated

case class AddressBookEntry(id: UUID, firstName: String, lastName:String, gender: Gender, dob: DateTime, version: Long)

case class GetEntry(id: UUID)

case object GetEntries

object AddressBook {
  val props = Props[AddressBook]
}

class AddressBook extends Actor {

  val model = scala.collection.mutable.Map[UUID, AddressBookEntry]()

  def receive = {
    case GetEntry(id) => sender ! model.get(id)
    case GetEntries => sender ! model.values.toSeq
    case Event(id, seqNum, PersonCreated(firstName, lastName, gender, dob)) => {
      model.put(id, AddressBookEntry(id, firstName, lastName, gender, dob, seqNum))
    }
  }
}