package com.example.fibonacci;

import kalix.javasdk.testkit.ActionResult;
import kalix.springsdk.testkit.ActionTestkit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FibonacciActionTest {

  @Test
  public void testNextFib() {
    ActionTestkit<FibonacciAction> testkit = ActionTestkit.of(FibonacciAction::new);
    ActionResult<Number> result = testkit.call(a -> a.nextNumber(3L));
    assertTrue(result.isReply());
    assertEquals(5L, result.getReply().value());
  }

  @Test
  public void testNextFibError() {
    ActionTestkit<FibonacciAction> testkit = ActionTestkit.of(FibonacciAction::new);
    ActionResult<Number> result = testkit.call(a -> a.nextNumber(4L));
    assertTrue(result.isError());
    assertTrue(result.getError().startsWith("Input number is not a Fibonacci number"));
  }
}
