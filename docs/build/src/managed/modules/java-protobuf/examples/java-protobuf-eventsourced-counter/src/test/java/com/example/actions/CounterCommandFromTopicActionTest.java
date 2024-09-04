package com.example.actions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterCommandFromTopicActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    CounterCommandFromTopicActionTestKit service = CounterCommandFromTopicActionTestKit.of(CounterCommandFromTopicAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void increaseTest() {
    CounterCommandFromTopicActionTestKit testKit = CounterCommandFromTopicActionTestKit.of(CounterCommandFromTopicAction::new);
    // ActionResult<Empty> result = testKit.increase(CounterApi.IncreaseValue.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void decreaseTest() {
    CounterCommandFromTopicActionTestKit testKit = CounterCommandFromTopicActionTestKit.of(CounterCommandFromTopicAction::new);
    // ActionResult<Empty> result = testKit.decrease(CounterApi.DecreaseValue.newBuilder()...build());
  }

}
