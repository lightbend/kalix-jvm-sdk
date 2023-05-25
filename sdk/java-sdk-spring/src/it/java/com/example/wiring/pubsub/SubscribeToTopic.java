/*
 * Copyright 2021 Lightbend Inc.
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

import com.example.wiring.eventsourcedentities.counter.CounterEvent;
import com.google.common.collect.Lists;
import io.grpc.Status;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static com.example.wiring.pubsub.PublishToTopic.COUNTER_EVENTS_TOPIC;

@Subscribe.Topic(COUNTER_EVENTS_TOPIC)
@Profile("docker-it-test")
public class SubscribeToTopic extends Action {

  private Logger logger = LoggerFactory.getLogger(getClass());

  public Effect<CounterEvent> handleIncrease(CounterEvent.ValueIncreased increased) {
    addEvent(increased);
    return effects().ignore();
  }

  public Effect<CounterEvent> handleMultiply(CounterEvent.ValueMultiplied multiplied) {
    addEvent(multiplied);
    return effects().ignore();
  }

  private void addEvent(CounterEvent counterEvent) {
    var entityId = actionContext().metadata().get("ce-subject").orElseThrow();
    logger.info("Consuming " + counterEvent + " from " + entityId);
    DummyCounterEventStore.store(entityId, counterEvent);
  }

  @GetMapping("/subscribe-to-topic/{entityId}")
  public Effect<List<CounterEvent>> get(@PathVariable String entityId) {
    List<CounterEvent> message = DummyCounterEventStore.get(entityId);
    return effects().reply(message);
  }
}
