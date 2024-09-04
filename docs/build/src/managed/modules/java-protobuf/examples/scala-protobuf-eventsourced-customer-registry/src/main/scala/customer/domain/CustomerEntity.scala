package customer.domain

import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.google.protobuf.empty.Empty
import customer.api

class CustomerEntity(context: EventSourcedEntityContext) extends AbstractCustomerEntity {

  private val entityId = context.entityId

  override def emptyState: CustomerState = CustomerState(customerId = entityId)

  override def getCustomer(
      currentState: CustomerState,
      getCustomerRequest: api.GetCustomerRequest): EventSourcedEntity.Effect[api.Customer] =
    effects.reply(convertToApi(currentState))

  override def create(currentState: CustomerState, customer: api.Customer): EventSourcedEntity.Effect[Empty] = {
    val event = CustomerCreated(Some(convertToDomain(customer)))
    effects.emitEvent(event).thenReply(_ => Empty.defaultInstance)
  }

  override def changeName(
      currentState: CustomerState,
      changeNameRequest: api.ChangeNameRequest): EventSourcedEntity.Effect[Empty] = {
    val event = CustomerNameChanged(newName = changeNameRequest.newName)
    effects.emitEvent(event).thenReply(_ => Empty.defaultInstance)
  }

  override def changeAddress(
      currentState: CustomerState,
      changeAddressRequest: api.ChangeAddressRequest): EventSourcedEntity.Effect[Empty] = {
    val event = CustomerAddressChanged(newAddress = changeAddressRequest.newAddress.map(convertToDomain))
    effects.emitEvent(event).thenReply(_ => Empty.defaultInstance)
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

  private def convertToApi(customer: CustomerState): api.Customer =
    api.Customer(
      customerId = customer.customerId,
      name = customer.name,
      email = customer.email,
      address = customer.address.map(convertToApi))

  private def convertToApi(address: Address): api.Address =
    api.Address(street = address.street, city = address.city)

  private def convertToDomain(customer: api.Customer): CustomerState =
    CustomerState(
      customerId = customer.customerId,
      email = customer.email,
      name = customer.name,
      address = customer.address.map(convertToDomain))

  private def convertToDomain(address: api.Address): Address =
    Address(street = address.street, city = address.city)
}
