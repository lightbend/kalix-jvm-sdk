package org.example.eventsourcedentity;

import com.google.protobuf.Empty;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.testkit.EventSourcedResult;
import org.junit.Test;

import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterTest {

  @Test
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
  public void increaseTest() {
    CounterTestKit testKit = CounterTestKit.of(Counter::new);
    // EventSourcedResult<Empty> result = testKit.increase(IncreaseValue.newBuilder()...build());
  }


  @Test
  public void decreaseTest() {
    CounterTestKit testKit = CounterTestKit.of(Counter::new);
    // EventSourcedResult<Empty> result = testKit.decrease(DecreaseValue.newBuilder()...build());
  }

}
