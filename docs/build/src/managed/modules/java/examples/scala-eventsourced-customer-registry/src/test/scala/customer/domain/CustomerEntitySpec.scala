package customer.domain

import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.testkit.EventSourcedResult
import com.google.protobuf.empty.Empty
import customer.api
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CustomerEntitySpec extends AnyWordSpec with Matchers {
  "The CustomerEntity" should {
    "have example test that can be removed" in {
      val testKit = CustomerEntityTestKit(new CustomerEntity(_))
      // use the testkit to execute a command:
      // val result: EventSourcedResult[R] = testKit.someOperation(SomeRequest("id"));
      // verify the emitted events
      // val actualEvent: ExpectedEvent = result.nextEventOfType[ExpectedEvent]
      // actualEvent shouldBe expectedEvent
      // verify the final state after applying the events
      // testKit.state() shouldBe expectedState
      // verify the response
      // result.reply shouldBe expectedReply
      // verify the final state after the command
    }

    "correctly process commands of type Create" in {
      val testKit = CustomerEntityTestKit(new CustomerEntity(_))
      // val result: EventSourcedResult[Empty] = testKit.create(api.Customer(...))
    }

    "correctly process commands of type ChangeName" in {
      val testKit = CustomerEntityTestKit(new CustomerEntity(_))
      // val result: EventSourcedResult[Empty] = testKit.changeName(api.ChangeNameRequest(...))
    }

    "correctly process commands of type ChangeAddress" in {
      val testKit = CustomerEntityTestKit(new CustomerEntity(_))
      // val result: EventSourcedResult[Empty] = testKit.changeAddress(api.ChangeAddressRequest(...))
    }

    "correctly process commands of type GetCustomer" in {
      val testKit = CustomerEntityTestKit(new CustomerEntity(_))
      // val result: EventSourcedResult[api.Customer] = testKit.getCustomer(api.GetCustomerRequest(...))
    }
  }
}
