package com.example.domain;

import com.akkaserverless.javasdk.testkit.EventSourcedResult;
import com.example.actions.CounterTopicApi;
import com.example.actions.CounterJournalToTopicAction;
import com.google.protobuf.Empty;
import org.junit.Test;

import static org.junit.Assert.*;

public class CounterJournalToTopicActionTest {

  @Test
  public void increaseTest() {
    CounterJournalToTopicActionTestKit testKit = CounterJournalToTopicActionTestKit.of(CounterJournalToTopicAction::new);
    EventSourcedResult<CounterTopicApi.Increased> result = testKit.increase(CounterDomain.ValueIncreased.newBuilder().setValue(1).build());
    assertTrue(result.didEmitEvents());
  }

}