package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import kalix.javasdk.testkit.ActionResult;
import org.example.service.MyServiceAction;
import org.example.service.MyServiceActionTestKit;
import org.example.service.ServiceOuterClass;
import org.external.ExternalDomain;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class MyServiceActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    MyServiceActionTestKit service = MyServiceActionTestKit.of(MyServiceAction::new);
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
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    // ActionResult<ExternalDomain.Empty> result = testKit.simpleMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void streamedOutputMethodTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    // Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> result = testKit.streamedOutputMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void streamedInputMethodTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    // ActionResult<ExternalDomain.Empty> result = testKit.streamedInputMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
  }

  @Test
  @Disabled("to be implemented")
  public void fullStreamedMethodTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    // Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> result = testKit.fullStreamedMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
  }

}
