package com.example.actions;

import com.example.Counter;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Subscribe.Topic(value = "counter-commands", ignoreUnknown = true)
public class CounterCommandFromTopicAction extends Action {

  public record IncreaseCounter(String counterId, int value) {
  }

  public record MultiplyCounter(String counterId, int value) {
  }

  private ComponentClient componentClient;

  public CounterCommandFromTopicAction(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  private Logger logger = LoggerFactory.getLogger(CounterCommandFromTopicAction.class);

  public Effect<String> onValueIncreased(IncreaseCounter increase) {
    logger.info("Received increase command: " + increase.toString());
    var deferredCall = componentClient.forEventSourcedEntity(increase.counterId).call(Counter::increase).params(increase.value);
    return effects().forward(deferredCall);
  }

  public Effect<String> onValueMultiplied(MultiplyCounter increase) {
    logger.info("Received increase command: " + increase.toString());
    var deferredCall = componentClient.forEventSourcedEntity(increase.counterId).call(Counter::multiply).params(increase.value);
    return effects().forward(deferredCall);
  }
}
