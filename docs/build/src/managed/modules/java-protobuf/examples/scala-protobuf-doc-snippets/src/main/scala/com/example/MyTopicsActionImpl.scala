package com.example

import com.google.protobuf.empty.Empty
import com.google.protobuf.wrappers.BytesValue
import com.google.protobuf.wrappers.StringValue
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class MyTopicsActionImpl(creationContext: ActionCreationContext) extends AbstractMyTopicsAction {

  override def consumeStringTopic(stringValue: StringValue): Action.Effect[Empty] = {
    throw new RuntimeException("The command handler for `ConsumeStringTopic` is not implemented, yet")
  }
  override def consumeRawBytesTopic(bytesValue: BytesValue): Action.Effect[Empty] = {
    throw new RuntimeException("The command handler for `ConsumeRawBytesTopic` is not implemented, yet")
  }
  override def protobufFromTopic(topicOperation: TopicOperation): Action.Effect[Empty] = {
    throw new RuntimeException("The command handler for `ProtobufFromTopic` is not implemented, yet")
  }
}

