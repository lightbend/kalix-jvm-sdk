package com.example;

import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import static com.example.CounterEvent.ValueIncreased;
import static com.example.CounterEvent.ValueMultiplied;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CounterTest {

  @Test
  public void testIncrease() {
    EventSourcedTestKit<Integer, CounterEvent, Counter> testKit = EventSourcedTestKit.of(Counter::new);
    EventSourcedResult<String> result = testKit.call(e -> e.increase(10));

    assertTrue(result.isReply());
    assertEquals("10", result.getReply());
    assertEquals(1, result.getAllEvents().size());
    result.getNextEventOfType(ValueIncreased.class);
    assertEquals(10, testKit.getState());
  }

  @Test
  public void testMultiply() {
    EventSourcedTestKit<Integer, CounterEvent, Counter> testKit = EventSourcedTestKit.of(Counter::new);
    // set initial value to 2
    testKit.call(e -> e.increase(2));

    EventSourcedResult<String> result = testKit.call(e -> e.multiply(10));
    assertTrue(result.isReply());
    assertEquals("20", result.getReply());
    assertEquals(1, result.getAllEvents().size());
    result.getNextEventOfType(ValueMultiplied.class);
    assertEquals(20, testKit.getState());
  }
}
