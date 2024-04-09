/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.action;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.spring.testmodels.Done;
import kalix.spring.testmodels.valueentity.Counter;
import kalix.spring.testmodels.valueentity.CounterState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/subscription/log")
public class CounterSubscriber extends Action {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Subscribe.ValueEntity(Counter.class)
  @PostMapping("/counter")
  public Effect<Done> changes(@RequestBody CounterState counterState) {
    logger.info("Counter subscriber: counter id '{}' is '{}'", counterState.id, counterState.value);
    return effects().reply(Done.instance);
  }
}
