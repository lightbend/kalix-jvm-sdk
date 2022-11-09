package customer.domain

import com.google.protobuf.empty.Empty
import customer.api
import kalix.scalasdk.testkit.ValueEntityResult
import kalix.scalasdk.valueentity.ValueEntity
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CustomerSpec
  extends AnyWordSpec
    with Matchers {

  "Customer" must {

    "have example test that can be removed" in {
      val service = CustomerTestKit(new Customer(_))
      pending
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = service.someOperation(SomeRequest)
      // verify the reply
      // val reply = result.getReply()
      // reply shouldBe expectedReply
      // verify the final state after the command
      // service.currentState() shouldBe expectedState
    }

    "handle command Create" in {
      val service = CustomerTestKit(new Customer(_))
      pending
      // val result = service.create(api.Customer(...))
    }

    "handle command GetCustomer" in {
      val service = CustomerTestKit(new Customer(_))
      pending
      // val result = service.getCustomer(api.GetCustomerRequest(...))
    }

    "handle command ChangeName" in {
      val service = CustomerTestKit(new Customer(_))
      pending
      // val result = service.changeName(api.ChangeNameRequest(...))
    }

    "handle command ChangeAddress" in {
      val service = CustomerTestKit(new Customer(_))
      pending
      // val result = service.changeAddress(api.ChangeAddressRequest(...))
    }

    "handle command Delete" in {
      val service = CustomerTestKit(new Customer(_))
      pending
      // val result = service.delete(api.DeleteCustomerRequest(...))
    }

  }
}
