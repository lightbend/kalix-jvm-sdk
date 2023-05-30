package customer.api;

import akka.stream.javadsl.Source;
import customer.api.CustomerEventsApi;
import customer.api.CustomerEventsToTopicServiceAction;
import customer.api.CustomerEventsToTopicServiceActionTestKit;
import customer.domain.CustomerDomain;
import kalix.javasdk.testkit.ActionResult;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerEventsToTopicServiceActionTest {

  @Test
  @Ignore("to be implemented")
  public void exampleTest() {
    CustomerEventsToTopicServiceActionTestKit service = CustomerEventsToTopicServiceActionTestKit.of(CustomerEventsToTopicServiceAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Ignore("to be implemented")
  public void transformCustomerCreatedTest() {
    CustomerEventsToTopicServiceActionTestKit testKit = CustomerEventsToTopicServiceActionTestKit.of(CustomerEventsToTopicServiceAction::new);
    // ActionResult<CustomerEventsApi.Created> result = testKit.transformCustomerCreated(CustomerDomain.CustomerCreated.newBuilder()...build());
  }

  @Test
  @Ignore("to be implemented")
  public void transformCustomerNameChangedTest() {
    CustomerEventsToTopicServiceActionTestKit testKit = CustomerEventsToTopicServiceActionTestKit.of(CustomerEventsToTopicServiceAction::new);
    // ActionResult<CustomerEventsApi.NameChanged> result = testKit.transformCustomerNameChanged(CustomerDomain.CustomerNameChanged.newBuilder()...build());
  }

}
