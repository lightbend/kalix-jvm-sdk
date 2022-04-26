package org.example.service;

import akka.stream.javadsl.Source;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import org.example.service.MyServiceActionImpl;
import org.example.service.MyServiceActionImplTestKit;
import org.example.service.ServiceOuterClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class MyServiceActionImplTest {

  @Test
  @Ignore("to be implemented")
  public void exampleTest() {
    MyServiceActionImplTestKit testKit = MyServiceActionImplTestKit.of(MyServiceActionImpl::new);
    // use the testkit to execute a command
    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
  }

  @Test
  @Ignore("to be implemented")
  public void simpleMethodTest() {
    MyServiceActionImplTestKit testKit = MyServiceActionImplTestKit.of(MyServiceActionImpl::new);
    // ActionResult<Empty> result = testKit.simpleMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
  }

}
