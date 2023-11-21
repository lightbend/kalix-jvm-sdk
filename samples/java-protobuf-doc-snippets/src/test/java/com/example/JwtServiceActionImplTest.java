package com.example;

import akka.stream.javadsl.Source;
import com.example.JwtService;
import com.example.JwtServiceActionImpl;
import com.example.JwtServiceActionImplTestKit;
import kalix.javasdk.testkit.ActionResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class JwtServiceActionImplTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    JwtServiceActionImplTestKit service = JwtServiceActionImplTestKit.of(JwtServiceActionImpl::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void jwtInTokenTest() {
    JwtServiceActionImplTestKit testKit = JwtServiceActionImplTestKit.of(JwtServiceActionImpl::new);
    // ActionResult<JwtService.MyResponse> result = testKit.jwtInToken(JwtService.MyRequest.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void jwtInMessageTest() {
    JwtServiceActionImplTestKit testKit = JwtServiceActionImplTestKit.of(JwtServiceActionImpl::new);
    // ActionResult<JwtService.MyResponse> result = testKit.jwtInMessage(JwtService.MyRequestWithToken.newBuilder()...build());
  }

}
