package model.command

import com.dreweaster.thespian.domain.DomainModel

object AddressBookDomain {
  val model = DomainModel("address-book") {
    Person
  }
}
