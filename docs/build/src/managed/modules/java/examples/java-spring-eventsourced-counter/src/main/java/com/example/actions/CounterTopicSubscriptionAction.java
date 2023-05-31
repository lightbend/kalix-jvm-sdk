package com.example.actions;

import com.example.CounterEvent.ValueIncreased;
import com.example.CounterEvent.ValueMultiplied;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// tag::class[]
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;

public class CounterTopicSubscriptionAction extends Action {

  private Logger logger = LoggerFactory.getLogger(CounterTopicSubscriptionAction.class);

  @Subscribe.Topic(value = "counter-events") // <1>
  public Action.Effect<Confirmed> onValueIncreased(ValueIncreased event) { // <2>
    logger.info("Received increased event: " + event.toString());
    return effects().reply(Confirmed.instance); // <3>
  }

  @Subscribe.Topic(value = "counter-events") // <4>
  public Action.Effect<Confirmed> onValueMultiplied(ValueMultiplied event) { // <5>
    logger.info("Received multiplied event: " + event.toString());
    return effects().reply(Confirmed.instance); // <6>
  }
}
// end::class[]