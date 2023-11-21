package org.example.valueentity.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.valueentity.ValueEntity;
import org.example.valueentity.CounterApi;
import org.example.valueentity.state.OuterCounterState;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    CounterTestKit service = CounterTestKit.of(Counter::new);
    // // use the testkit to execute a command
    // // of events emitted, or a final updated state:
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ValueEntityResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
    // // verify the final state after the command
    // assertEquals(expectedState, service.getState());
  }

  @Test
  @Disabled("to be implemented")
  public void increaseTest() {
    CounterTestKit service = CounterTestKit.of(Counter::new);
    // IncreaseValue command = IncreaseValue.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.increase(command);
  }


  @Test
  @Disabled("to be implemented")
  public void decreaseTest() {
    CounterTestKit service = CounterTestKit.of(Counter::new);
    // DecreaseValue command = DecreaseValue.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.decrease(command);
  }

}
