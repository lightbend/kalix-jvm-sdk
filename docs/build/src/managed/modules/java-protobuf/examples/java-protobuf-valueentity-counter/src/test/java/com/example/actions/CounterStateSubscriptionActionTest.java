package com.example.actions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterStateSubscriptionActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    CounterStateSubscriptionActionTestKit service = CounterStateSubscriptionActionTestKit.of(CounterStateSubscriptionAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void onUpdateStateTest() {
    CounterStateSubscriptionActionTestKit testKit = CounterStateSubscriptionActionTestKit.of(CounterStateSubscriptionAction::new);
    // ActionResult<Empty> result = testKit.onUpdateState(CounterDomain.CounterState.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void onDeleteEntityTest() {
    CounterStateSubscriptionActionTestKit testKit = CounterStateSubscriptionActionTestKit.of(CounterStateSubscriptionAction::new);
    // ActionResult<Empty> result = testKit.onDeleteEntity();
  }

}
