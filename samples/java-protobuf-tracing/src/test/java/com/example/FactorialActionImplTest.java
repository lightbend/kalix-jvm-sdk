package com.example;

import com.example.FactorialActionApi;
import com.example.FactorialActionImplTestKit;
import com.example.FactorialApi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class FactorialActionImplTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    FactorialActionImplTestKit service = FactorialActionImplTestKit.of(FactorialActionImpl::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void calculateFactorialTest() {
    FactorialActionImplTestKit testKit = FactorialActionImplTestKit.of(FactorialActionImpl::new);
    // ActionResult<FactorialApi.FactorialResponse> result = testKit.calculateFactorial(FactorialActionApi.CalculateFactorialRequest.newBuilder()...build());
  }

}
