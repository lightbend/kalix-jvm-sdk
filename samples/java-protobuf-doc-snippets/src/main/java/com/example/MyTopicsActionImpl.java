package com.example;

import com.google.protobuf.BytesValue;
import com.google.protobuf.Empty;
import com.google.protobuf.StringValue;
import kalix.javasdk.action.ActionCreationContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your com/example/topics_action.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class MyTopicsActionImpl extends AbstractMyTopicsAction {

  public MyTopicsActionImpl(ActionCreationContext creationContext) {}

  @Override
  public Effect<Empty> consumeStringTopic(StringValue stringValue) {
    throw new RuntimeException("The command handler for `ConsumeStringTopic` is not implemented, yet");
  }
  @Override
  public Effect<Empty> consumeRawBytesTopic(BytesValue bytesValue) {
    throw new RuntimeException("The command handler for `ConsumeRawBytesTopic` is not implemented, yet");
  }
  @Override
  public Effect<Empty> protobufFromTopic(MyTopics.TopicOperation topicOperation) {
    throw new RuntimeException("The command handler for `ProtobufFromTopic` is not implemented, yet");
  }
}
