package org.example.eventsourcedentity.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.testkit.EventSourcedResult;
import org.example.eventsourcedentity.CounterApi;
import org.example.eventsourcedentity.events.OuterCounterEvents;
import org.example.eventsourcedentity.state.OuterCounterState;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterTest {

  @Test
  @Ignore("to be implemented")
  public void exampleTest() {
    CounterTestKit testKit = CounterTestKit.of(Counter::new);
    // use the testkit to execute a command
    // of events emitted, or a final updated state:
    // EventSourcedResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the emitted events
    // ExpectedEvent actualEvent = result.getNextEventOfType(ExpectedEvent.class);
    // assertEquals(expectedEvent, actualEvent)
    // verify the final state after applying the events
    // assertEquals(expectedState, testKit.getState());
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  @Ignore("to be implemented")
  public void increaseTest() {
    CounterTestKit testKit = CounterTestKit.of(Counter::new);
    // EventSourcedResult<Empty> result = testKit.increase(IncreaseValue.newBuilder()...build());
  }


  @Test
  @Ignore("to be implemented")
  public void decreaseTest() {
    CounterTestKit testKit = CounterTestKit.of(Counter::new);
    // EventSourcedResult<Empty> result = testKit.decrease(DecreaseValue.newBuilder()...build());
  }

}
