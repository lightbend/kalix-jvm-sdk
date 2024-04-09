/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.pubsub;

import com.example.wiring.eventsourcedentities.counter.CounterEntity;
import com.example.wiring.eventsourcedentities.counter.CounterEvent;
import com.example.wiring.eventsourcedentities.counter.CounterEvent.ValueIncreased;
import com.example.wiring.eventsourcedentities.counter.CounterEvent.ValueMultiplied;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import static kalix.javasdk.impl.MetadataImpl.CeSubject;

@Profile("docker-it-test")
@Subscribe.EventSourcedEntity(value = CounterEntity.class, ignoreUnknown = true)
public class PublishESToTopic extends Action {

  public static final String COUNTER_EVENTS_TOPIC = "counter-events";
  private Logger logger = LoggerFactory.getLogger(getClass());

  @Publish.Topic(COUNTER_EVENTS_TOPIC)
  public Effect<CounterEvent> handleIncrease(ValueIncreased increased) {
    return publish(increased);
  }

  @Publish.Topic(COUNTER_EVENTS_TOPIC)
  public Effect<CounterEvent> handleMultiply(ValueMultiplied multiplied) {
    return publish(multiplied);
  }

  private Effect<CounterEvent> publish(CounterEvent counterEvent) {
    String entityId = actionContext().metadata().get(CeSubject()).orElseThrow();
    logger.info("Publishing to " + COUNTER_EVENTS_TOPIC + " event: " + counterEvent + " from " + entityId);
    return effects().reply(counterEvent, Metadata.EMPTY.add(CeSubject(), entityId));
  }
}
