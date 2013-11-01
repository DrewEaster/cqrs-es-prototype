package model.command

import com.dreweaster.thespian.domain.DomainModel

object ExampleDomain {
  val model = DomainModel("example-domain") {
    Person
  }
}
