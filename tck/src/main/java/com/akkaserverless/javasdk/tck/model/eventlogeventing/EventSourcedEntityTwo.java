/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.eventlogeventing;

import com.akkaserverless.javasdk.eventsourced.CommandContext;
import com.akkaserverless.javasdk.eventsourced.CommandHandler;
import com.akkaserverless.javasdk.eventsourced.EventHandler;
import com.akkaserverless.javasdk.eventsourced.EventSourcedEntity;
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
