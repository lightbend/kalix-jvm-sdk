package customer.api;

import akka.stream.javadsl.Source;
import customer.api.CustomerEventsApi;
import customer.api.CustomerEventsServiceAction;
import customer.api.CustomerEventsServiceActionTestKit;
import customer.domain.CustomerDomain;
import kalix.javasdk.testkit.ActionResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerEventsServiceActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    CustomerEventsServiceActionTestKit service = CustomerEventsServiceActionTestKit.of(CustomerEventsServiceAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void transformCustomerCreatedTest() {
    CustomerEventsServiceActionTestKit testKit = CustomerEventsServiceActionTestKit.of(CustomerEventsServiceAction::new);
    // ActionResult<CustomerEventsApi.Created> result = testKit.transformCustomerCreated(CustomerDomain.CustomerCreated.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void transformCustomerNameChangedTest() {
    CustomerEventsServiceActionTestKit testKit = CustomerEventsServiceActionTestKit.of(CustomerEventsServiceAction::new);
    // ActionResult<CustomerEventsApi.NameChanged> result = testKit.transformCustomerNameChanged(CustomerDomain.CustomerNameChanged.newBuilder()...build());
  }

}
