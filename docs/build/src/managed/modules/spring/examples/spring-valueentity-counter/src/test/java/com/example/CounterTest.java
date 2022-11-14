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

  // tag::example[]
  @Test
  public void testSetAndIncrease() {
    ValueEntityTestKit<Integer, CounterEntity> testKit = ValueEntityTestKit.of(CounterEntity::new); // <1>
    ValueEntityResult<Number> resultSet = testKit.call(e -> e.set(new Number(10))); // <2>
    assertTrue(resultSet.isReply());
    assertEquals(10, resultSet.getReply().value()); // <3>

    ValueEntityResult<Number> resultPlusOne = testKit.call(CounterEntity::plusOne); // <4>
    assertTrue(resultPlusOne.isReply());
    assertEquals(11, resultPlusOne.getReply().value());

    assertEquals(11, testKit.getState()); // <5>
  }
  // end::example[]
}
