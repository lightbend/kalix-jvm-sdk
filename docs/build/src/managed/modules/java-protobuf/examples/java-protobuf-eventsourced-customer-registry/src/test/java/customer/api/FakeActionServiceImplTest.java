package customer.api;

import akka.stream.javadsl.Source;
import com.google.protobuf.Empty;
import customer.api.FakeActionServiceImpl;
import customer.api.FakeActionServiceImplTestKit;
import kalix.javasdk.testkit.ActionResult;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class FakeActionServiceImplTest {

  @Test
  @Ignore("to be implemented")
  public void exampleTest() {
    FakeActionServiceImplTestKit service = FakeActionServiceImplTestKit.of(FakeActionServiceImpl::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Ignore("to be implemented")
  public void createTest() {
    FakeActionServiceImplTestKit testKit = FakeActionServiceImplTestKit.of(FakeActionServiceImpl::new);
    // ActionResult<Empty> result = testKit.create(Empty.newBuilder()...build());
  }

}
