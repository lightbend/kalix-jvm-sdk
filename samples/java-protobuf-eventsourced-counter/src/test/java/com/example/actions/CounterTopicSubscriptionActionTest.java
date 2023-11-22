package com.example.actions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterTopicSubscriptionActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    CounterTopicSubscriptionActionTestKit service = CounterTopicSubscriptionActionTestKit.of(CounterTopicSubscriptionAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void onIncreasedTest() {
    CounterTopicSubscriptionActionTestKit testKit = CounterTopicSubscriptionActionTestKit.of(CounterTopicSubscriptionAction::new);
    // ActionResult<Empty> result = testKit.onIncreased(CounterTopicApi.Increased.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void onDecreasedTest() {
    CounterTopicSubscriptionActionTestKit testKit = CounterTopicSubscriptionActionTestKit.of(CounterTopicSubscriptionAction::new);
    // ActionResult<Empty> result = testKit.onDecreased(CounterTopicApi.Decreased.newBuilder()...build());
  }

}
