package com.example.actions

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.google.protobuf.empty.Empty

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An action. */
class DoubleCounterAction(creationContext: ActionCreationContext) extends AbstractDoubleCounterAction {

  /** Handler for "Increase". */
  override def increase(empty: Empty): Action.Effect[Empty] = {
    throw new RuntimeException("The command handler for `Increase` is not implemented, yet")
  }
}
