package customer.action;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerActionImplTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    CustomerActionImplTestKit service = CustomerActionImplTestKit.of(CustomerActionImpl::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void createTest() {
    CustomerActionImplTestKit testKit = CustomerActionImplTestKit.of(CustomerActionImpl::new);
    // ActionResult<Empty> result = testKit.create(CustomerActionProto.Customer.newBuilder()...build());
  }

}
