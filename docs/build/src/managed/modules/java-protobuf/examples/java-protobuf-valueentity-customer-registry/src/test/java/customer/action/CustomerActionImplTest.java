package customer.action;

import akka.stream.javadsl.Source;
import kalix.javasdk.testkit.ActionResult;
import com.google.protobuf.Empty;
import customer.action.CustomerActionImpl;
import customer.action.CustomerActionImplTestKit;
import customer.api.CustomerApi;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CustomerActionImplTest {

  @Test
  public void exampleTest() {
    CustomerActionImplTestKit testKit = CustomerActionImplTestKit.of(CustomerActionImpl::new);
    // use the testkit to execute a command
    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void createTest() {
    CustomerActionImplTestKit testKit = CustomerActionImplTestKit.of(CustomerActionImpl::new);
    // ActionResult<Empty> result = testKit.create(CustomerApi.Customer.newBuilder()...build());
  }

}
