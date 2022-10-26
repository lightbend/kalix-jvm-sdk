package com.example;

import kalix.javasdk.testkit.ValueEntityResult;
import kalix.springsdk.testkit.ValueEntityTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CounterTest {

  @Test
  public void testIncrease() {
    ValueEntityTestKit<Integer, CounterEntity> testKit = ValueEntityTestKit.of(CounterEntity::new);
    ValueEntityResult<Number> result = testKit.call(e -> e.increaseBy(new Number(10)));

    assertTrue(result.isReply());
    assertEquals(10, result.getReply().value());
    assertEquals(10, testKit.getState());
  }

}
