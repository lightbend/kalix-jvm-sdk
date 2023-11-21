package com.example;

import akka.stream.javadsl.Source;
import com.example.MyTopics;
import com.example.MyTopicsActionImpl;
import com.example.MyTopicsActionImplTestKit;
import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import kalix.javasdk.testkit.ActionResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class MyTopicsActionImplTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    MyTopicsActionImplTestKit service = MyTopicsActionImplTestKit.of(MyTopicsActionImpl::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void consumeStringTopicTest() {
    MyTopicsActionImplTestKit testKit = MyTopicsActionImplTestKit.of(MyTopicsActionImpl::new);
    // ActionResult<Empty> result = testKit.consumeStringTopic(StringValue.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void consumeRawBytesTopicTest() {
    MyTopicsActionImplTestKit testKit = MyTopicsActionImplTestKit.of(MyTopicsActionImpl::new);
    // ActionResult<Empty> result = testKit.consumeRawBytesTopic(BytesValue.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void protobufFromTopicTest() {
    MyTopicsActionImplTestKit testKit = MyTopicsActionImplTestKit.of(MyTopicsActionImpl::new);
    // ActionResult<Empty> result = testKit.protobufFromTopic(MyTopics.TopicOperation.newBuilder()...build());
  }

}
