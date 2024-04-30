/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.pubsub;


import com.example.wiring.valueentities.customer.CustomerEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import kalix.javasdk.JsonSupport;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

@Profile("docker-it-test")
@Subscribe.ValueEntity(CustomerEntity.class)
public class PublishBytesToTopic extends Action {

  public static final String CUSTOMERS_BYTES_TOPIC = "customers_bytes";
  private Logger logger = LoggerFactory.getLogger(getClass());

  @Publish.Topic(CUSTOMERS_BYTES_TOPIC)
  public Action.Effect<byte[]> handleChange(CustomerEntity.Customer customer) {
    try {
      var payload = JsonSupport.getObjectMapper().writerFor(CustomerEntity.Customer.class).writeValueAsBytes(customer);
      logger.info("Publishing to " + CUSTOMERS_BYTES_TOPIC + " raw bytes: " + new String(payload));
      return effects().reply(payload);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
