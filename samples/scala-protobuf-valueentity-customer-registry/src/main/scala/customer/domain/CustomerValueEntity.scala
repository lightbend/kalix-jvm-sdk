/* This code was generated by Kalix tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package customer.domain

import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import com.google.protobuf.empty.Empty
import customer.api
import customer.api.DeleteCustomerRequest

class CustomerValueEntity(context: ValueEntityContext) extends AbstractCustomerValueEntity {

  override def emptyState: CustomerState = CustomerState()

  override def create(currentState: CustomerState, command: api.Customer): ValueEntity.Effect[Empty] = {
    val state = convertToDomain(command)
    effects.updateState(state).thenReply(Empty())
  }

  override def changeName(
      currentState: CustomerState,
      command: api.ChangeNameRequest): ValueEntity.Effect[Empty] = {
    val updatedState = currentState.copy(name = command.newName)
    effects.updateState(updatedState).thenReply(Empty())
  }

  override def changeAddress(
      currentState: CustomerState,
      command: api.ChangeAddressRequest): ValueEntity.Effect[Empty] = {
    val updatedState = currentState.copy(address = command.newAddress.map(convertAddressToDomain))
    effects.updateState(updatedState).thenReply(Empty())
  }

  override def delete(
      currentState: CustomerState,
      command: api.DeleteCustomerRequest): ValueEntity.Effect[Empty] = {
    effects.deleteEntity().thenReply(Empty())
  }

  override def getCustomer(
      currentState: CustomerState,
      command: api.GetCustomerRequest): ValueEntity.Effect[api.Customer] =
    effects.reply(convertToApi(currentState))

  private def convertToDomain(customer: api.Customer) =
    CustomerState(
      customerId = customer.customerId,
      email = customer.email,
      name = customer.name,
      address = customer.address.map(convertAddressToDomain))

  private def convertAddressToDomain(address: api.Address) =
    Address(street = address.street, city = address.city)

  private def convertToApi(state: CustomerState) =
    api.Customer(
      customerId = state.customerId,
      email = state.email,
      name = state.name,
      address = state.address.map(address => api.Address(street = address.street, city = address.city)))

}