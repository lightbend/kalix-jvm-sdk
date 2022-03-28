package com.example.actions;

import kalix.javasdk.testkit.ActionResult;
import com.example.actions.CounterTopicApi;
import com.example.actions.CounterTopicSubscriptionAction;
import com.example.actions.CounterTopicSubscriptionActionTestKit;
import com.google.protobuf.Empty;
import org.junit.Test;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterTopicSubscriptionActionTest {

  @Test
  public void exampleTest() {
    CounterTopicSubscriptionActionTestKit testKit = CounterTopicSubscriptionActionTestKit.of(CounterTopicSubscriptionAction::new);
    // use the testkit to execute a command
    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void increaseTest() {
    CounterTopicSubscriptionActionTestKit testKit = CounterTopicSubscriptionActionTestKit.of(CounterTopicSubscriptionAction::new);
    // ActionResult<Empty> result = testKit.increase(CounterTopicApi.Increased.newBuilder()...build());
  }

  @Test
  public void decreaseTest() {
    CounterTopicSubscriptionActionTestKit testKit = CounterTopicSubscriptionActionTestKit.of(CounterTopicSubscriptionAction::new);
    // ActionResult<Empty> result = testKit.decrease(CounterTopicApi.Decreased.newBuilder()...build());
  }

}