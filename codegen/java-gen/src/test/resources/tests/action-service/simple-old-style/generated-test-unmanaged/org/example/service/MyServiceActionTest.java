package org.example.service;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.testkit.ActionResult;
import org.example.service.MyServiceAction;
import org.example.service.MyServiceActionTestKit;
import org.example.service.ServiceOuterClass;
import org.external.ExternalDomain;
import org.junit.Test;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class MyServiceActionTest {

  @Test
  public void exampleTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    // use the testkit to execute a command
    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  public void simpleMethodTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    // ActionResult<ExternalDomain.Empty> result = testKit.simpleMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
  }

  @Test
  public void streamedOutputMethodTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    // Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> result = testKit.streamedOutputMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
  }

  @Test
  public void streamedInputMethodTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    // ActionResult<ExternalDomain.Empty> result = testKit.streamedInputMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
  }

  @Test
  public void fullStreamedMethodTest() {
    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
    // Source<ActionResult<ExternalDomain.Empty>, akka.NotUsed> result = testKit.fullStreamedMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
  }

}
