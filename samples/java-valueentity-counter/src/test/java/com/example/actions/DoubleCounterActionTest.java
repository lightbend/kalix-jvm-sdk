package com.example.actions;

import com.akkaserverless.javasdk.testkit.ActionResult;
import com.example.CounterApi;
import com.example.actions.DoubleCounterAction;
import com.example.actions.DoubleCounterActionTestKit;
import com.google.protobuf.Empty;
import org.junit.Test;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class DoubleCounterActionTest {

  @Test
  public void exampleTest() {
    DoubleCounterActionTestKit testKit = DoubleCounterActionTestKit.of(DoubleCounterAction::new);
    // use the testkit to execute a command
    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void increaseTest() {
    DoubleCounterActionTestKit testKit = DoubleCounterActionTestKit.of(DoubleCounterAction::new);
    // ActionResult<Empty> result = testKit.increase(CounterApi.IncreaseValue.newBuilder()...build());
  }

}
