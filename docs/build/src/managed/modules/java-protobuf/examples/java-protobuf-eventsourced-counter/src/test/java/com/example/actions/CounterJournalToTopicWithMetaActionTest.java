package com.example.actions;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterJournalToTopicWithMetaActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    CounterJournalToTopicWithMetaActionTestKit service = CounterJournalToTopicWithMetaActionTestKit.of(CounterJournalToTopicWithMetaAction::new);
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
    CounterJournalToTopicWithMetaActionTestKit testKit = CounterJournalToTopicWithMetaActionTestKit.of(CounterJournalToTopicWithMetaAction::new);
    // ActionResult<CounterTopicApi.Increased> result = testKit.onIncreased(CounterDomain.ValueIncreased.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void onDecreasedTest() {
    CounterJournalToTopicWithMetaActionTestKit testKit = CounterJournalToTopicWithMetaActionTestKit.of(CounterJournalToTopicWithMetaAction::new);
    // ActionResult<CounterTopicApi.Decreased> result = testKit.onDecreased(CounterDomain.ValueDecreased.newBuilder()...build());
  }

}
