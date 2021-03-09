/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.valuebased;

import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.tck.model.valueentity.Valueentity.Request;
import com.akkaserverless.tck.model.valueentity.Valueentity.Response;

@ValueEntity(entityType = "value-entity-configured")
public class ValueEntityConfiguredEntity {

  @CommandHandler
  public Response call(Request request) {
    return Response.getDefaultInstance();
  }
}
