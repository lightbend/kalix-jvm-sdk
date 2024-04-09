/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.pubsub;

import com.example.wiring.eventsourcedentities.counter.CounterEvent;
import com.example.wiring.valueentities.customer.CustomerEntity;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

import static com.example.wiring.pubsub.PublishVEToTopic.CUSTOMERS_TOPIC;
import static kalix.javasdk.StatusCode.ErrorCode.NOT_FOUND;

@Profile("docker-it-test")
@Subscribe.Topic(CUSTOMERS_TOPIC)
public class SubscribeToCustomersTopic extends Action {

  private Logger logger = LoggerFactory.getLogger(getClass());

  public Effect<CounterEvent> handle(CustomerEntity.Customer customer) {
    var entityId = actionContext().metadata().get("ce-subject").orElseThrow();
    logger.info("Consuming " + customer + " from " + entityId);
    DummyCustomerStore.store(CUSTOMERS_TOPIC, entityId, customer);
    return effects().ignore();
  }
}
