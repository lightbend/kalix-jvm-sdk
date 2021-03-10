/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.eventlogeventing;

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.tck.model.Eventlogeventing;
import com.google.protobuf.Empty;

@EventSourcedEntity(entityType = "eventlogeventing-one")
public class EventSourcedEntityOne {
  @CommandHandler
  public Empty emitEvent(Eventlogeventing.EmitEventRequest event, CommandContext ctx) {
    if (event.hasEventOne()) {
      ctx.emit(event.getEventOne());
    } else {
      ctx.emit(event.getEventTwo());
    }
    return Empty.getDefaultInstance();
  }

  @EventHandler
  public void handle(Eventlogeventing.EventOne event) {}

  @EventHandler
  public void handle(Eventlogeventing.EventTwo event) {}
}
