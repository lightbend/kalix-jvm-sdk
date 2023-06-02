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
    logger.info("Consuming raw bytes: " + new String(payload));
    try {
      Object o = JsonSupport.getObjectMapper().readerFor(CustomerEntity.Customer.class).readValue(payload);
      System.out.println(o);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return effects().reply(Done.done());
  }

  @GetMapping("/subscribe-to-customer-bytes-topic/{entityId}")
  public Effect<CustomerEntity.Customer> get(@PathVariable String entityId) {
    CustomerEntity.Customer customer = DummyCustomerStore.get(entityId);
    if (customer != null) {
      return effects().reply(customer);
    } else {
      return effects().error("not found " + entityId, NOT_FOUND);
    }
  }
}
