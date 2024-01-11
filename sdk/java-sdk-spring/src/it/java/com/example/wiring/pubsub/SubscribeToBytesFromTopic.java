/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
