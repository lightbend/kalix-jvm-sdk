package com.example;

import akka.stream.javadsl.Source;
import com.example.DelegatingServiceAction;
import com.example.DelegatingServiceActionTestKit;
import com.example.DelegatingServiceApi;
import kalix.javasdk.testkit.ActionResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class DelegatingServiceActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    DelegatingServiceActionTestKit service = DelegatingServiceActionTestKit.of(DelegatingServiceAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void addAndReturnTest() {
    DelegatingServiceActionTestKit testKit = DelegatingServiceActionTestKit.of(DelegatingServiceAction::new);
    // ActionResult<DelegatingServiceApi.Result> result = testKit.addAndReturn(DelegatingServiceApi.Request.newBuilder()...build());
  }

}
