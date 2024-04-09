/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.pubsub;

import com.example.wiring.valueentities.customer.CustomerEntity;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import static kalix.javasdk.impl.MetadataImpl.CeSubject;

@Profile({"docker-it-test", "eventing-testkit-destination"})
@Subscribe.ValueEntity(CustomerEntity.class)
public class PublishVEToTopic extends Action {

  public static final String CUSTOMERS_TOPIC = "customers";
  private Logger logger = LoggerFactory.getLogger(getClass());

  @Publish.Topic(CUSTOMERS_TOPIC)
  public Effect<CustomerEntity.Customer> handleChange(CustomerEntity.Customer customer) {
    String entityId = actionContext().metadata().get(CeSubject()).orElseThrow();
    logger.info("Publishing to " + CUSTOMERS_TOPIC + " message: " + customer + " from " + entityId);
    return effects().reply(customer, Metadata.EMPTY.add(CeSubject(), entityId));
  }
}
