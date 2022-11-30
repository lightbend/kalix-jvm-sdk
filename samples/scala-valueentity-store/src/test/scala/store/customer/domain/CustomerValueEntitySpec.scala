package store.customer.domain

import com.google.protobuf.empty.Empty
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import store.customer.api

class CustomerValueEntitySpec extends AnyWordSpec with Matchers {

  "CustomerValueEntity" must {

    "handle Create and Get commands" in {
      val service = CustomerValueEntityTestKit(new CustomerValueEntity(_))
      val customer = api.Customer(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(api.Address("123 Some Street", "Some City")))
      val createResult = service.create(customer)
      createResult.reply shouldBe Empty.defaultInstance
      service.currentState() shouldBe CustomerState(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(Address("123 Some Street", "Some City")))
      val getResult = service.get(api.GetCustomer("C001"))
      getResult.reply shouldBe customer
    }

  }
}
