package customer.view

import kalix.scalasdk.view.View.UpdateEffect
import kalix.scalasdk.view.ViewContext
import com.google.protobuf.any.{ Any => ScalaPbAny }
import customer.api
import customer.api.Customer
import customer.domain
import customer.domain.CustomerAddressChanged
import customer.domain.CustomerCreated
import customer.domain.CustomerNameChanged
import customer.domain.CustomerState

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CustomerByCityStreamingView(context: ViewContext) extends AbstractCustomerByCityStreamingView {

  override def emptyState: Customer = Customer()

  override def processCustomerCreated(state: Customer, customerCreated: CustomerCreated): UpdateEffect[Customer] =
    if (state != emptyState) effects.ignore() // already created
    else effects.updateState(convertToApi(customerCreated.customer.get))

  override def processCustomerNameChanged(
      state: Customer,
      customerNameChanged: CustomerNameChanged): UpdateEffect[Customer] =
    effects.updateState(state.copy(name = customerNameChanged.newName))

  override def processCustomerAddressChanged(
      state: Customer,
      customerAddressChanged: CustomerAddressChanged): UpdateEffect[Customer] = // <3>
    effects.updateState(state.copy(address = customerAddressChanged.newAddress.map(convertToApi)))

  private def convertToApi(customer: CustomerState): Customer =
    Customer(
      customerId = customer.customerId,
      name = customer.name,
      email = customer.email,
      address = customer.address.map(convertToApi))

  private def convertToApi(address: domain.Address): api.Address =
    api.Address(street = address.street, city = address.city)

}
