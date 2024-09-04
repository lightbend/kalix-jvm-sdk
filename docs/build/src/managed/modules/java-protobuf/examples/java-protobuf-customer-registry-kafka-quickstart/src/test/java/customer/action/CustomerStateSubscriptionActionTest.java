package customer.action;

import akka.stream.javadsl.Source;
import kalix.javasdk.testkit.ActionResult;
import customer.action.CustomerStateSubscriptionAction;
import customer.action.CustomerStateSubscriptionActionTestKit;
import customer.domain.CustomerDomain;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerStateSubscriptionActionTest {

  @Test
  public void exampleTest() {
    CustomerStateSubscriptionActionTestKit testKit = CustomerStateSubscriptionActionTestKit.of(CustomerStateSubscriptionAction::new);
    // use the testkit to execute a command
    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void onStateChangeTest() {
    CustomerStateSubscriptionActionTestKit testKit = CustomerStateSubscriptionActionTestKit.of(CustomerStateSubscriptionAction::new);
    // ActionResult<CustomerDomain.CustomerState> result = testKit.onUpsertState(CustomerDomain.CustomerState.newBuilder()...build());
  }

}
