package com.example.actions

import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionCreationContext
import com.google.protobuf.empty.Empty
import org.slf4j.LoggerFactory

// tag::counter-topic-sub[]
class CounterTopicSubscriptionAction(creationContext: ActionCreationContext)
    extends AbstractCounterTopicSubscriptionAction {
  private val logger = LoggerFactory.getLogger(getClass())
  /** Handler for "Increase". */
  override def increase(increased: Increased): Action.Effect[Empty] = {
    logger.info("Received increase event: " + increased.toString())
    effects.reply(Empty.defaultInstance)
  }

  override def decrease(decreased: Decreased): Action.Effect[Empty] = {
    logger.info("Received decrease event: " + decreased.toString())
    effects.reply(Empty.defaultInstance)
  }
}
// end::counter-topic-sub[]