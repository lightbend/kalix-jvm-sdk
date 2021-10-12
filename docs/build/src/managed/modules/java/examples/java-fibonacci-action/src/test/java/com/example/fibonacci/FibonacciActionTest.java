package com.example.fibonacci;

import com.akkaserverless.javasdk.testkit.ActionResult;
import com.example.fibonacci.FibonacciAction;
import com.example.fibonacci.FibonacciActionTestKit;
import com.example.fibonacci.FibonacciApi;
import org.junit.Test;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::class[]
public class FibonacciActionTest {

  @Test
  public void nextNumberTest() {
    FibonacciActionTestKit testKit = FibonacciActionTestKit.of(FibonacciAction::new); // <1>
    ActionResult<FibonacciApi.Number> result = testKit.nextNumber(FibonacciApi.Number.newBuilder().setValue(5).build()); // <2>
    assertEquals(8, result.getReply().getValue()); // <3>
  }

}
// end::class[]