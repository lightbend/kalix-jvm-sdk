package com.example.actions;

import akka.stream.javadsl.Source;
import com.example.actions.TestAction;
import com.example.actions.TestActionTestKit;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class TestActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    TestActionTestKit service = TestActionTestKit.of(TestAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void runTestTest() {
    TestActionTestKit testKit = TestActionTestKit.of(TestAction::new);
    // ActionResult<Empty> result = testKit.runTest(Empty.newBuilder()...build());
  }

}
