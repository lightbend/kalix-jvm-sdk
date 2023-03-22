package customer.api

import customer.domain
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CustomerEventsServiceAction(creationContext: ActionCreationContext) extends AbstractCustomerEventsServiceAction {

  // transform internal entity event types to public API events
  // tag::transform[]
  override def transformCustomerCreated(customerCreated: domain.CustomerCreated): Action.Effect[Created] = {
    val customer = customerCreated.getCustomer
    effects.reply(Created(customer.customerId, customer.name, customer.email))
  }

  override def transformCustomerNameChanged(
      customerNameChanged: domain.CustomerNameChanged): Action.Effect[NameChanged] = {
    // Note: customer_id is not present in the event or elsewhere here, but will be available as subject id
    // from the metadata on the consuming side
    effects.reply(NameChanged(customerNameChanged.newName))
  }
  // end::transform[]
}
