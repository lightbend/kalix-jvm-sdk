package com.example.actions;

import com.akkaserverless.javasdk.testkit.ActionResult;
import com.akkaserverless.javasdk.testkit.ServiceCallDetails;
import com.example.CounterApi;
import com.example.actions.DoubleCounterAction;
import com.example.actions.DoubleCounterActionTestKit;
import com.google.protobuf.Empty;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class DoubleCounterActionTest {
// tag::side-effect-test[]
  @Test
// end::side-effect-test[]
  public void exampleTest() {
    DoubleCounterActionTestKit testKit = DoubleCounterActionTestKit.of(DoubleCounterAction::new);
    // use the testkit to execute a command
    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }


  //FIX modify DoubleCounterAction and the .proto so it has a method
  // we can call to test sideEffects 
  @Ignore
// tag::side-effect-test[]
  public void increaseWithSideEffectTest() {
    DoubleCounterActionTestKit testKit = DoubleCounterActionTestKit.of(DoubleCounterAction::new);
    int increase = 3;
    CounterApi.IncreaseValue increaseValueCommand = CounterApi.IncreaseValue.newBuilder()
        .setValue(increase)
        .build();
    ActionResult<Empty> result1 = testKit.increase(increaseValueCommand);// <1>
    ServiceCallDetails sideEffect = result1.getSideEffects().get(0);// <2>
    assertEquals("com.example.CounterService", sideEffect.getServiceName());// <3>
    assertEquals("Increase", sideEffect.getMethodName());// <4>
    CounterApi.IncreaseValue doubledIncreased =  CounterApi.IncreaseValue.newBuilder()
        .setValue(increase * 2)
        .build();
    assertEquals(doubledIncreased, sideEffect.getMessage());// <5>
  }
  // end::side-effect-test[]

}
