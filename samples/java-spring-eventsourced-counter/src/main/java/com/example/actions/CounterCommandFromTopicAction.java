package com.example.actions;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.spring.KalixClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Subscribe.Topic(value = "counter-commands", ignoreUnknown = true)
public class CounterCommandFromTopicAction extends Action {

  public record IncreaseCounter(String counterId, int value) { }
  public record MultiplyCounter(String counterId, int value) { }

  private KalixClient kalixClient;

  public CounterCommandFromTopicAction(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }

  private Logger logger = LoggerFactory.getLogger(CounterCommandFromTopicAction.class);

  public Effect<String> onValueIncreased(IncreaseCounter increase) {
    logger.info("Received increase command: " + increase.toString());
    var deferredCall = kalixClient.post("/counter/"+ increase.counterId + "/increase/" + increase.value, String.class);
    return effects().forward(deferredCall);
  }

  public Effect<String> onValueDecreased(MultiplyCounter increase) {
    logger.info("Received increase command: " + increase.toString());
    var deferredCall = kalixClient.post("/counter/"+ increase.counterId + "/multiply/" + increase.value, String.class);
    return effects().forward(deferredCall);
  }

}
