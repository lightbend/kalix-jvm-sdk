package com.example.actions

import com.example.{DecreaseValue, IncreaseValue}
import com.google.protobuf.empty.Empty
import kalix.scalasdk.action.{Action, ActionCreationContext}
import org.slf4j.LoggerFactory

class CounterCommandFromTopicAction(creationContext: ActionCreationContext)
    extends AbstractCounterCommandFromTopicAction {
  private val logger = LoggerFactory.getLogger(getClass())

  override def increase(increase: IncreaseValue): Action.Effect[Empty] = {
    logger.info("Received increase event: " + increase);
    val increaseCmd = components.counter.increase(increase)
    effects.forward(increaseCmd)
  }

  override def decrease(decrease: DecreaseValue): Action.Effect[Empty] = {
    logger.info("Received decrease event: " + decrease.toString)
    val decreaseCmd = components.counter.decrease(decrease)
    effects.forward(decreaseCmd)
  }
}
