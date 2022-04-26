package org.example.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.valueentity.ValueEntity;
import org.example.state.OuterCounterState;
import org.example.valueentity.CounterApi;
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
    // ValueEntityResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
    // verify the final state after the command
    // assertEquals(expectedState, testKit.getState());
  }

  @Test
  @Ignore("to be implemented")
  public void increaseTest() {
    CounterTestKit testKit = CounterTestKit.of(Counter::new);
    // ValueEntityResult<Empty> result = testKit.increase(IncreaseValue.newBuilder()...build());
  }


  @Test
  @Ignore("to be implemented")
  public void decreaseTest() {
    CounterTestKit testKit = CounterTestKit.of(Counter::new);
    // ValueEntityResult<Empty> result = testKit.decrease(DecreaseValue.newBuilder()...build());
  }

}
