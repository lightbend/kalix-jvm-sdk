package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import kalix.javasdk.testkit.ActionResult;
import com.google.protobuf.Empty;
import org.example.service.MyServiceNamedAction;
import org.example.service.MyServiceNamedActionTestKit;
import org.example.service.ServiceOuterClass;
import org.junit.Test;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class MyServiceNamedActionTest {

  @Test
  public void exampleTest() {
    MyServiceNamedActionTestKit testKit = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // use the testkit to execute a command
    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void simpleMethodTest() {
    MyServiceNamedActionTestKit testKit = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // ActionResult<Empty> result = testKit.simpleMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
  }

  @Test
  public void streamedOutputMethodTest() {
    MyServiceNamedActionTestKit testKit = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // Source<ActionResult<Empty>, akka.NotUsed> result = testKit.streamedOutputMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
  }

  @Test
  public void streamedInputMethodTest() {
    MyServiceNamedActionTestKit testKit = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // ActionResult<Empty> result = testKit.streamedInputMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
  }

  @Test
  public void fullStreamedMethodTest() {
    MyServiceNamedActionTestKit testKit = MyServiceNamedActionTestKit.of(MyServiceNamedAction::new);
    // Source<ActionResult<Empty>, akka.NotUsed> result = testKit.fullStreamedMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
  }

}
