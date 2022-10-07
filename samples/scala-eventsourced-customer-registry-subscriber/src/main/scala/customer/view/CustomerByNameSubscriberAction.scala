package customer.view

import com.google.protobuf.empty.Empty
import customer.api.Created
import customer.api.Customer
import customer.api.NameChanged
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CustomerByNameSubscriberAction(creationContext: ActionCreationContext) extends AbstractCustomerByNameSubscriberAction {

  private val log = LoggerFactory.getLogger(classOf[CustomerByNameSubscriberAction])

  override def processCustomerCreated(created: Created): Action.Effect[Empty] = {
    log.info("Customer {} created: {}", actionContext.eventSubject, created)
    effects.reply(Empty.defaultInstance)
  }
  override def processCustomerNameChanged(nameChanged: NameChanged): Action.Effect[Empty] = {
    log.info("Customer {} name changed: {}", actionContext.eventSubject, nameChanged)
    effects.reply(Empty.defaultInstance)
  }
}

