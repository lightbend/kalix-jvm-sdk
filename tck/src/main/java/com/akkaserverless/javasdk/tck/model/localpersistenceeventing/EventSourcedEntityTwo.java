/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Empty;

@EventSourcedEntity(entityType = "eventlogeventing-two")
public class EventSourcedEntityTwo {
  @CommandHandler
  public Empty emitJsonEvent(LocalPersistenceEventing.JsonEvent event, CommandContext ctx) {
    ctx.emit(new JsonMessage(event.getMessage()));
    return Empty.getDefaultInstance();
  }

  @EventHandler
  public void handle(JsonMessage message) {}
}
