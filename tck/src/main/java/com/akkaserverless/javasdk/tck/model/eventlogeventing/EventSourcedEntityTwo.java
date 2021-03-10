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

@EventSourcedEntity(entityType = "eventlogeventing-two")
public class EventSourcedEntityTwo {
  @CommandHandler
  public Empty emitJsonEvent(Eventlogeventing.JsonEvent event, CommandContext ctx) {
    ctx.emit(new JsonMessage(event.getMessage()));
    return Empty.getDefaultInstance();
  }

  @EventHandler
  public void handle(JsonMessage message) {}
}
