package com.example.actions

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.ActionCreationContext
import com.google.protobuf.empty.Empty
import org.slf4j.LoggerFactory

// tag::counter-topic-sub[]
class CounterTopicSubscriptionAction(creationContext: ActionCreationContext)
    extends AbstractCounterTopicSubscriptionAction {
  private val logger = LoggerFactory.getLogger(getClass())
  /** Handler for "Increase". */
  override def increase(increased: Increased): Action.Effect[Empty] = {
    logger.info("Received increase event: " + increased.toString())
    effects.noReply
  }

  override def decrease(decreased: Decreased): Action.Effect[Empty] = {
    logger.info("Received decrease event: " + decreased.toString())
    effects.noReply
  }
}
// end::counter-topic-sub[]