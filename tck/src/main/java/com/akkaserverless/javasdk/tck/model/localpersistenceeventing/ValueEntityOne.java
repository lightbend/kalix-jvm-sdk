/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Empty;

@ValueEntity(entityType = "valuechangeseventing-one")
public class ValueEntityOne {
  @CommandHandler
  public Empty updateValue(
      LocalPersistenceEventing.UpdateValueRequest value, CommandContext<Object> ctx) {
    if (value.hasValueOne()) {
      ctx.updateState(value.getValueOne());
    } else {
      ctx.updateState(value.getValueTwo());
    }
    return Empty.getDefaultInstance();
  }
}
