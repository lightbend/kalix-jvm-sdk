package com.example;

import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.testkit.ValueEntityTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CounterTest {

  @Test
  public void testIncrease() {
    var testKit = ValueEntityTestKit.of(CounterEntity::new);
    var result = testKit.call(e -> e.increaseBy(new Number(10)));

    assertTrue(result.isReply());
    assertEquals(10, result.getReply().value());
    assertEquals(10, testKit.getState());
  }

  // tag::example[]
  @Test
  public void testSetAndIncrease() {
    var testKit = ValueEntityTestKit.of(CounterEntity::new); // <1>

    var resultSet = testKit.call(e -> e.set(new Number(10))); // <2>
    assertTrue(resultSet.isReply());
    assertEquals(10, resultSet.getReply().value()); // <3>

    var resultPlusOne = testKit.call(CounterEntity::plusOne); // <4>
    assertTrue(resultPlusOne.isReply());
    assertEquals(11, resultPlusOne.getReply().value());

    assertEquals(11, testKit.getState()); // <5>
  }
  // end::example[]

  @Test
  public void testDelete() {
    var testKit = ValueEntityTestKit.of(CounterEntity::new);
    testKit.call(e -> e.increaseBy(new Number(10)));

    testKit.call(e -> e.delete());

    assertEquals(0, testKit.getState());
  }
}
