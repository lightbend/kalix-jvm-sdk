package store.customer.domain

import com.google.protobuf.empty.Empty
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import store.customer.api

class CustomerEntity(context: EventSourcedEntityContext) extends AbstractCustomerEntity {

  override def emptyState: CustomerState = CustomerState.defaultInstance

  override def create(currentState: CustomerState, customer: api.Customer): EventSourcedEntity.Effect[Empty] = {
    val customerState = CustomerState(
      customerId = customer.customerId,
      email = customer.email,
      name = customer.name,
      address = customer.address.map(address => Address(street = address.street, city = address.city)))
    val customerCreated = CustomerCreated(Some(customerState))
    effects.emitEvent(customerCreated).thenReply(_ => Empty.defaultInstance)
  }

  override def get(
      currentState: CustomerState,
      getCustomer: api.GetCustomer): EventSourcedEntity.Effect[api.Customer] = {
    val customer = api.Customer(
      customerId = currentState.customerId,
      email = currentState.email,
      name = currentState.name,
      address = currentState.address.map(address => api.Address(street = address.street, city = address.city)))
    effects.reply(customer)
  }

  override def changeName(
      currentState: CustomerState,
      changeCustomerName: api.ChangeCustomerName): EventSourcedEntity.Effect[Empty] = {
    val customerNameChanged = CustomerNameChanged(newName = changeCustomerName.newName)
    effects.emitEvent(customerNameChanged).thenReply(_ => Empty.defaultInstance)
  }

  override def changeAddress(
      currentState: CustomerState,
      changeCustomerAddress: api.ChangeCustomerAddress): EventSourcedEntity.Effect[Empty] = {
    val customerAddressChanged = CustomerAddressChanged(newAddress =
      changeCustomerAddress.newAddress.map(address => Address(street = address.street, city = address.city)))
    effects.emitEvent(customerAddressChanged).thenReply(_ => Empty.defaultInstance)
  }

  override def customerCreated(currentState: CustomerState, customerCreated: CustomerCreated): CustomerState =
    customerCreated.customer.get

  override def customerNameChanged(
      currentState: CustomerState,
      customerNameChanged: CustomerNameChanged): CustomerState =
    currentState.copy(name = customerNameChanged.newName)

  override def customerAddressChanged(
      currentState: CustomerState,
      customerAddressChanged: CustomerAddressChanged): CustomerState =
    currentState.copy(address = customerAddressChanged.newAddress)

}
