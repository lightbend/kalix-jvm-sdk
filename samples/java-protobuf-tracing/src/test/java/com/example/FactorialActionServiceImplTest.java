package com.example;

import com.example.FactorialActionServiceImplTestKit;
import com.example.FactorialApi;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class FactorialActionServiceImplTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    FactorialActionServiceImplTestKit service = FactorialActionServiceImplTestKit.of(FactorialActionServiceImpl::new);
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
    FactorialActionServiceImplTestKit testKit = FactorialActionServiceImplTestKit.of(FactorialActionServiceImpl::new);
    // ActionResult<FactorialApi.FactorialResponse> result = testKit.calculateFactorial(FactorialApi.CalculateFactorialRequest.newBuilder()...build());
  }

}
