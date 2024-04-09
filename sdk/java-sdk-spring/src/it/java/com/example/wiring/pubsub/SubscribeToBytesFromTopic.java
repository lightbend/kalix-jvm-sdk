/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.pubsub;


import akka.Done;
import com.example.wiring.valueentities.customer.CustomerEntity;
import kalix.javasdk.JsonSupport;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;

import static com.example.wiring.pubsub.PublishBytesToTopic.CUSTOMERS_BYTES_TOPIC;
import static kalix.javasdk.StatusCode.ErrorCode.NOT_FOUND;

@Profile("docker-it-test")
@Subscribe.Topic(CUSTOMERS_BYTES_TOPIC)
public class SubscribeToBytesFromTopic extends Action {

  private Logger logger = LoggerFactory.getLogger(getClass());

  public Effect<Done> handleChange(byte[] payload) {
    try {
      logger.info("Consuming raw bytes: " + new String(payload));
      CustomerEntity.Customer customer = JsonSupport.getObjectMapper().readerFor(CustomerEntity.Customer.class).readValue(payload);
      DummyCustomerStore.store(CUSTOMERS_BYTES_TOPIC, customer.name(), customer);
      return effects().reply(Done.done());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
