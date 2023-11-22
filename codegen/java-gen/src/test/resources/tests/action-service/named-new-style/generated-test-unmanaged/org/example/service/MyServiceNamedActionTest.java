package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import org.example.service.MyServiceNamedAction;
import org.example.service.MyServiceNamedActionTestKit;
import org.example.service.ServiceOuterClass;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class MyServiceNamedActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    MyServiceNamedActionTestKit service = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void simpleMethodTest() {
    MyServiceNamedActionTestKit testKit = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // ActionResult<Empty> result = testKit.simpleMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void streamedOutputMethodTest() {
    MyServiceNamedActionTestKit testKit = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // Source<ActionResult<Empty>, akka.NotUsed> result = testKit.streamedOutputMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void streamedInputMethodTest() {
    MyServiceNamedActionTestKit testKit = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // ActionResult<Empty> result = testKit.streamedInputMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
  }

  @Test
  @Disabled("to be implemented")
  public void fullStreamedMethodTest() {
    MyServiceNamedActionTestKit testKit = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // Source<ActionResult<Empty>, akka.NotUsed> result = testKit.fullStreamedMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
  }

}
