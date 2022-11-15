package com.example;

import com.example.action.Confirmed;
import com.example.action.DoubleCounterAction;
import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.springsdk.KalixClient;
import kalix.springsdk.testkit.ActionTestkit;
import kalix.springsdk.testkit.ValueEntityTestKit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class CounterTest {


  @Test
  public void testIncrease() {
    ValueEntityTestKit<Integer, CounterEntity> testKit = ValueEntityTestKit.of(CounterEntity::new);
    ValueEntityResult<Number> result = testKit.call(e -> e.increaseBy(new Number(10)));

    assertTrue(result.isReply());
    assertEquals(10, result.getReply().value());
    assertEquals(10, testKit.getState());
  }


  @Test
  public void testIncreaseWithSideEffects() {
    KalixClient kalixClient = mock(KalixClient.class);

    //DELETE var def = new RestDeferredCall("request", MetadataImpl.Empty(), "full.service.Name", "MethodName", () -> new Number(2));
    ActionTestkit<DoubleCounterAction> testKit = ActionTestkit.of(() -> new DoubleCounterAction(kalixClient));
    ActionResult<Confirmed> result = testKit.call(e -> e.increaseWithSideEffect(new Number(2)), Metadata.EMPTY.set("ce-subject","1"));
    result.getSideEffects().get(0).getMethodName(); // DELETE this is null as the KalixClient is not creating the deferred call on the DoubleCounterAction.
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