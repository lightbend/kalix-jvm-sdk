package com.example.actions

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.google.protobuf.empty.Empty
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An action. */
class CounterTopicSubscriptionAction(creationContext: ActionCreationContext)
    extends AbstractCounterTopicSubscriptionAction {
  private val logger = LoggerFactory.getLogger(getClass())
  /** Handler for "Increase". */
  override def increase(increased: Increased): Action.Effect[Empty] = {
    logger.info("Received increase event: " + increased.toString())
    effects.noReply
  }

  /** Handler for "Decrease". */
  override def decrease(decreased: Decreased): Action.Effect[Empty] = {
    logger.info("Received increase event: " + decreased.toString())
    effects.noReply
  }
}
