package com.example.fibonacci;

// tag::testing-action[]
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.ActionTestkit;
import org.junit.jupiter.api.Test;

// end::testing-action[]

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// tag::testing-action[]
public class FibonacciActionTest {

  @Test
  public void testNextFib() {
    ActionTestkit<FibonacciAction> testkit = ActionTestkit.of(FibonacciAction::new); // <1>
    ActionResult<Number> result = testkit.call(a -> a.getNumber(3L));  // <2>
    assertTrue(result.isReply());
    assertEquals(5L, result.getReply().value());
  }

  @Test
  public void testNextFibError() {
    ActionTestkit<FibonacciAction> testkit = ActionTestkit.of(FibonacciAction::new);  // <1>
    ActionResult<Number> result = testkit.call(a -> a.getNumber(4L));     // <2>
    assertTrue(result.isError());
    assertTrue(result.getError().startsWith("Input number is not a Fibonacci number"));
  }
}
// end::testing-action[]
