package customer.view

import customer.api.Created
import customer.api.Customer
import customer.api.NameChanged
import kalix.scalasdk.action.Action
import kalix.scalasdk.testkit.ActionResult
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CustomerByNameSubscriberActionSpec
    extends AnyWordSpec
    with Matchers {

  "CustomerByNameSubscriberAction" must {

    "have example test that can be removed" in {
      val service = CustomerByNameSubscriberActionTestKit(new CustomerByNameSubscriberAction(_))
      pending
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = service.someOperation(SomeRequest)
      // verify the reply
      // result.reply shouldBe expectedReply
    }

    "handle command ProcessCustomerCreated" in {
      val service = CustomerByNameSubscriberActionTestKit(new CustomerByNameSubscriberAction(_))
          pending
      // val result = service.processCustomerCreated(Created(...))
    }

    "handle command ProcessCustomerNameChanged" in {
      val service = CustomerByNameSubscriberActionTestKit(new CustomerByNameSubscriberAction(_))
          pending
      // val result = service.processCustomerNameChanged(NameChanged(...))
    }

  }
}
