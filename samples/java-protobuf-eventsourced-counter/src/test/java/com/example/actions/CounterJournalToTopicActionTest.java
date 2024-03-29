/* This code was generated by Kalix tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */
package com.example.actions;

import com.example.domain.CounterDomain;
import kalix.javasdk.testkit.ActionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class CounterJournalToTopicActionTest {

  @Test
  public void increaseTest() {
    CounterJournalToTopicActionTestKit testKit = CounterJournalToTopicActionTestKit.of(CounterJournalToTopicAction::new);
    ActionResult<CounterTopicApi.Increased> result = testKit.onIncreased(CounterDomain.ValueIncreased.newBuilder().setValue(1).build());
    assertEquals(1, result.getReply().getValue());
  }


  @Test
  public void decreaseTest() {
    CounterJournalToTopicActionTestKit testKit = CounterJournalToTopicActionTestKit.of(CounterJournalToTopicAction::new);
    ActionResult<CounterTopicApi.Decreased> result = testKit.onDecreased(CounterDomain.ValueDecreased.newBuilder().setValue(1).build());
    assertEquals(1, result.getReply().getValue());
  }
}
