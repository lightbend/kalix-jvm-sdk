package org.example.valueentity;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.valueentity.ValueEntity;
import org.example.valueentity.domain.CounterDomain;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterServiceEntityTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    CounterServiceEntityTestKit service = CounterServiceEntityTestKit.of(CounterServiceEntity::new);
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
    CounterServiceEntityTestKit service = CounterServiceEntityTestKit.of(CounterServiceEntity::new);
    // IncreaseValue command = IncreaseValue.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.increase(command);
  }


  @Test
  @Disabled("to be implemented")
  public void decreaseTest() {
    CounterServiceEntityTestKit service = CounterServiceEntityTestKit.of(CounterServiceEntity::new);
    // DecreaseValue command = DecreaseValue.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.decrease(command);
  }

}
