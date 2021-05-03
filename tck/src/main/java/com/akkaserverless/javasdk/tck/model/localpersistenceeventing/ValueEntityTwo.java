/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Empty;

@ValueEntity(entityType = "valuechangeseventing-two")
public class ValueEntityTwo {
  @CommandHandler
  public Empty updateJsonValue(
      LocalPersistenceEventing.JsonValue value, CommandContext<Object> ctx) {
    ctx.updateState(new JsonMessage(value.getMessage()));
    return Empty.getDefaultInstance();
  }
}
