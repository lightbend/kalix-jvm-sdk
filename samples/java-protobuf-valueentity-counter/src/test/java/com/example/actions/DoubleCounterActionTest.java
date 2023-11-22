package com.example.actions;

import com.example.CounterApi;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.DeferredCallDetails;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class DoubleCounterActionTest {

  // tag::side-effect-test[]
  @Test
  public void increaseWithSideEffectTest() {
    DoubleCounterActionTestKit testKit = DoubleCounterActionTestKit.of(DoubleCounterAction::new);
    int increase = 3;
    CounterApi.IncreaseValue increaseValueCommand = CounterApi.IncreaseValue.newBuilder()
        .setValue(increase)
        .build();
    ActionResult<Empty> result1 = testKit.increaseWithSideEffect(increaseValueCommand);// <1>
    DeferredCallDetails<?, ?> sideEffect = result1.getSideEffects().get(0);// <2>
    assertEquals("com.example.CounterService", sideEffect.getServiceName());// <3>
    assertEquals("Increase", sideEffect.getMethodName());// <4>
    CounterApi.IncreaseValue doubledIncreased =  CounterApi.IncreaseValue.newBuilder()
        .setValue(increase * 2)
        .build();
    assertEquals(doubledIncreased, sideEffect.getMessage());// <5>
  }
  // end::side-effect-test[]

  @Test
  public void increaseTest() {
    DoubleCounterActionTestKit testKit = DoubleCounterActionTestKit.of(DoubleCounterAction::new);
    ActionResult<Empty> result = testKit.increase(CounterApi.IncreaseValue.newBuilder().setValue(2).build());
    assertTrue(result.isForward());
    DeferredCallDetails<?, Empty> forward = result.getForward();
    assertEquals(CounterApi.IncreaseValue.newBuilder().setValue(4).build(), forward.getMessage());
    assertEquals("com.example.CounterService", forward.getServiceName());
    assertEquals("Increase", forward.getMethodName());
  }
// tag::side-effect-test[]
}
// end::side-effect-test[]
