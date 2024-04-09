/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.eventsourcedentities.counter;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.annotations.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Id("id")
@TypeId("counter-entity")
@RequestMapping("/counter/{id}")
public class CounterEntity extends EventSourcedEntity<Counter, CounterEvent> {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private final EventSourcedEntityContext context;

  public CounterEntity(EventSourcedEntityContext context) {
    this.context = context;
  }

  @Override
  public Counter emptyState() {
    return new Counter(0);
  }

  @PostMapping("/increase/{value}")
  public Effect<Integer> increase(@PathVariable Integer value) {
    return effects().emitEvent(new CounterEvent.ValueIncreased(value)).thenReply(c -> c.value());
  }

  @PostMapping("/set/{value}")
  public Effect<Integer> set(@PathVariable Integer value) {
    return effects().emitEvent(new CounterEvent.ValueSet(value)).thenReply(c -> c.value());
  }

  @PostMapping("/set")
  public Effect<Integer> setFromReqParam(@RequestParam Integer value) {
    return effects().emitEvent(new CounterEvent.ValueSet(value)).thenReply(c -> c.value());
  }

  @GetMapping
  public Effect<Integer> get() {
    // don't modify, we want to make sure we call currentState().value here
    return effects().reply(currentState().value());
  }

  @PostMapping("/multiply/{value}")
  public Effect<Integer> times(@PathVariable Integer value) {
    logger.info(
        "Increasing counter with commandId={} commandName={} seqNr={} current={} value={}",
        commandContext().commandId(),
        commandContext().commandName(),
        commandContext().sequenceNumber(),
        currentState(),
        value);

    return effects().emitEvent(new CounterEvent.ValueMultiplied(value)).thenReply(c -> c.value());
  }

  @PostMapping("/restart")
  public Effect<Integer> restart() { // force entity restart, useful for testing
    logger.info(
        "Restarting counter with commandId={} commandName={} seqNr={} current={}",
        commandContext().commandId(),
        commandContext().commandName(),
        commandContext().sequenceNumber(),
        currentState());

    throw new RuntimeException("Forceful restarting entity!");
  }

  @EventHandler
  public Counter handle(CounterEvent counterEvent) {
    return currentState().apply(counterEvent);
  }
}
