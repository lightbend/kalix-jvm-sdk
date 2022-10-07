package customer.view

import customer.api.Created
import customer.api.Customer
import customer.api.NameChanged
import kalix.scalasdk.view.View.UpdateEffect
import kalix.scalasdk.view.ViewContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** not currently working
class CustomerByNameView(context: ViewContext) extends AbstractCustomerByNameView {

  override def emptyState: Customer = Customer.defaultInstance

  override def processCustomerCreated(
    state: Customer, created: Created): UpdateEffect[Customer] =
    effects.updateState(state.copy(customerId = created.customerId, email = created.email, name = created.customerName))

  override def processCustomerNameChanged(
    state: Customer, nameChanged: NameChanged): UpdateEffect[Customer] =
    effects.updateState(state.copy(name = nameChanged.customerName))
}
  */
