package com.example.actions;

import org.junit.Ignore;
import org.junit.Test;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterTopicRawSubscriptionActionTest {

  @Test
  @Ignore("to be implemented")
  public void exampleTest() {
    CounterTopicRawSubscriptionActionTestKit service = CounterTopicRawSubscriptionActionTestKit.of(CounterTopicRawSubscriptionAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Ignore("to be implemented")
  public void onIncreasedTest() {
    CounterTopicRawSubscriptionActionTestKit testKit = CounterTopicRawSubscriptionActionTestKit.of(CounterTopicRawSubscriptionAction::new);
    // ActionResult<Empty> result = testKit.onIncreased(CounterTopicApi.Increased.newBuilder()...build());
  }

  @Test
  @Ignore("to be implemented")
  public void onDecreasedTest() {
    CounterTopicRawSubscriptionActionTestKit testKit = CounterTopicRawSubscriptionActionTestKit.of(CounterTopicRawSubscriptionAction::new);
    // ActionResult<Empty> result = testKit.onDecreased(CounterTopicApi.Decreased.newBuilder()...build());
  }

}
