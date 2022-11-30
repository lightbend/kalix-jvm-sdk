package store.customer.domain

import com.google.protobuf.empty.Empty
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import store.customer.api

class CustomerValueEntity(context: ValueEntityContext) extends AbstractCustomerValueEntity {

  override def emptyState: CustomerState = CustomerState.defaultInstance

  override def create(currentState: CustomerState, customer: api.Customer): ValueEntity.Effect[Empty] = {
    val customerState = CustomerState(
      customerId = customer.customerId,
      email = customer.email,
      name = customer.name,
      address = customer.address.map(address => Address(street = address.street, city = address.city)))
    effects.updateState(customerState).thenReply(Empty.defaultInstance)
  }

  override def get(currentState: CustomerState, getCustomer: api.GetCustomer): ValueEntity.Effect[api.Customer] = {
    val customer = api.Customer(
      customerId = currentState.customerId,
      email = currentState.email,
      name = currentState.name,
      address = currentState.address.map(address => api.Address(street = address.street, city = address.city)))
    effects.reply(customer)
  }
}
