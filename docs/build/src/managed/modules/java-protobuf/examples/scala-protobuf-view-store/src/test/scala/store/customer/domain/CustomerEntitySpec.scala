package store.customer.domain

import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import store.customer.api

class CustomerEntitySpec extends AnyWordSpec with Matchers {

  "CustomerEntity" should {

    "handle Create and Get commands" in {
      val service = CustomerEntityTestKit(new CustomerEntity(_))

      val customer = api.Customer(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(api.Address("123 Some Street", "Some City")))

      val customerState = CustomerState(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(Address("123 Some Street", "Some City")))

      val createResult = service.create(customer)
      createResult.reply shouldBe Empty.defaultInstance
      createResult.events.size shouldBe 1
      createResult.nextEvent[CustomerCreated] shouldBe CustomerCreated(customer = Some(customerState))

      service.currentState shouldBe customerState

      service.get(api.GetCustomer("C001")).reply shouldBe customer
    }

    "handle ChangeName command" in {
      val service = CustomerEntityTestKit(new CustomerEntity(_))

      val customer = api.Customer(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(api.Address("123 Some Street", "Some City")))

      val createResult = service.create(customer)
      createResult.reply shouldBe Empty.defaultInstance

      val customerState = CustomerState(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(Address("123 Some Street", "Some City")))

      service.currentState shouldBe customerState

      service.get(api.GetCustomer("C001")).reply shouldBe customer

      val changeNameResult = service.changeName(api.ChangeCustomerName(customerId = "C001", newName = "Some Name"))
      changeNameResult.reply shouldBe Empty.defaultInstance

      changeNameResult.events.size shouldBe 1
      changeNameResult.nextEvent[CustomerNameChanged] shouldBe CustomerNameChanged(newName = "Some Name")

      val customerStateWithNewName = customerState.withName("Some Name")
      service.currentState shouldBe customerStateWithNewName

      val customerWithNewName = customer.withName("Some Name")
      service.get(api.GetCustomer("C001")).reply shouldBe customerWithNewName
    }

    "handle ChangeAddress command" in {
      val service = CustomerEntityTestKit(new CustomerEntity(_))

      val customer = api.Customer(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(api.Address("123 Some Street", "Some City")))

      val createResult = service.create(customer)
      createResult.reply shouldBe Empty.defaultInstance

      val customerState = CustomerState(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(Address("123 Some Street", "Some City")))

      service.currentState shouldBe customerState

      service.get(api.GetCustomer("C001")).reply shouldBe customer

      val changeAddressResult =
        service.changeAddress(
          api.ChangeCustomerAddress(
            customerId = "C001",
            newAddress = Some(api.Address("42 Some Road", "Some Other City"))))
      changeAddressResult.reply shouldBe Empty.defaultInstance
      changeAddressResult.events.size shouldBe 1
      changeAddressResult.nextEvent[CustomerAddressChanged] shouldBe CustomerAddressChanged(newAddress =
        Some(Address("42 Some Road", "Some Other City")))

      val customerStateWithNewAddress = customerState.withAddress(Address("42 Some Road", "Some Other City"))
      service.currentState shouldBe customerStateWithNewAddress

      val customerWithNewAddress = customer.withAddress(api.Address("42 Some Road", "Some Other City"))
      service.get(api.GetCustomer("C001")).reply shouldBe customerWithNewAddress
    }

  }
}
