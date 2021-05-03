/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.eventsourcedentity;

import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.tck.model.EventSourcedEntity.Request;
import com.akkaserverless.tck.model.EventSourcedEntity.Response;

@EventSourcedEntity(entityType = "EventSourcedTwoEntity")
public class EventSourcedTwoEntity {

  @CommandHandler
  public Response call(Request request) {
    return Response.getDefaultInstance();
  }
}
