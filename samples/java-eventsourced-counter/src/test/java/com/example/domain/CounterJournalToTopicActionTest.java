package com.example.domain;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;
import com.akkaserverless.javasdk.testkit.ActionResult;
import com.example.actions.CounterTopicApi;
import com.example.actions.CounterJournalToTopicAction;
import com.google.protobuf.Empty;
import org.junit.Test;

import static org.junit.Assert.*;

public class CounterJournalToTopicActionTest {

  @Test
  public void increaseTest() {
    CounterJournalToTopicActionTestKit testKit = CounterJournalToTopicActionTestKit.of(CounterJournalToTopicAction::new);
    int valueToincrease = 1;
    ActionResult<CounterTopicApi.Increased> result = testKit.increase(CounterDomain.ValueIncreased.newBuilder().setValue(valueToincrease).build());
    assertTrue(result.isReply());
    CounterTopicApi.Increased replyMessage = result.getReplyMsg();
    assertEquals(replyMessage.getValue(),valueToincrease);
  }

}