package com.example.domain;

import com.example.FactorialApi;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.valueentity.ValueEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class FactorialTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    FactorialTestKit service = FactorialTestKit.of(Factorial::new);
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
  public void getFactorialTest() {
    FactorialTestKit service = FactorialTestKit.of(Factorial::new);
    // FactorialRequest command = FactorialRequest.newBuilder()...build();
    // ValueEntityResult<FactorialResponse> result = service.getFactorial(command);
  }

}
