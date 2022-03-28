package com.example.actions;

import kalix.javasdk.testkit.ActionResult;
import com.example.actions.CounterStateSubscriptionAction;
import com.example.actions.CounterStateSubscriptionActionTestKit;
import com.example.domain.CounterDomain;
import com.google.protobuf.Empty;
import org.junit.Test;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterStateSubscriptionActionTest {

  @Test
  public void exampleTest() {
    CounterStateSubscriptionActionTestKit testKit = CounterStateSubscriptionActionTestKit.of(CounterStateSubscriptionAction::new);
    // use the testkit to execute a command
    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void onUpdateStateTest() {
    CounterStateSubscriptionActionTestKit testKit = CounterStateSubscriptionActionTestKit.of(CounterStateSubscriptionAction::new);
    // ActionResult<Empty> result = testKit.onUpdateState(CounterDomain.CounterState.newBuilder()...build());
  }

}
