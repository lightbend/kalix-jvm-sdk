package com.example;

import akka.stream.javadsl.Source;
import com.example.ControllerAction;
import com.example.ControllerActionApi;
import com.example.ControllerActionTestKit;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class ControllerActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    ControllerActionTestKit service = ControllerActionTestKit.of(ControllerAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void callSyncEndpointTest() {
    ControllerActionTestKit testKit = ControllerActionTestKit.of(ControllerAction::new);
    // ActionResult<ControllerActionApi.MessageResponse> result = testKit.callSyncEndpoint(Empty.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void callAsyncEndpointTest() {
    ControllerActionTestKit testKit = ControllerActionTestKit.of(ControllerAction::new);
    // ActionResult<ControllerActionApi.MessageResponse> result = testKit.callAsyncEndpoint(Empty.newBuilder()...build());
  }

}
