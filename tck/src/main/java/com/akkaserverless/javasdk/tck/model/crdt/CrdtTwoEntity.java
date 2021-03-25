/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.crdt;

import com.akkaserverless.javasdk.crdt.CommandContext;
import com.akkaserverless.javasdk.crdt.CommandHandler;
import com.akkaserverless.javasdk.crdt.CrdtEntity;
import com.akkaserverless.javasdk.crdt.GCounter;
import com.akkaserverless.tck.model.CrdtEntity.Request;
import com.akkaserverless.tck.model.CrdtEntity.RequestAction;
import com.akkaserverless.tck.model.CrdtEntity.Response;

@CrdtEntity
public class CrdtTwoEntity {
  // create a CRDT to be able to call delete
  public CrdtTwoEntity(GCounter counter) {}

  @CommandHandler
  public Response call(Request request, CommandContext context) {
    for (RequestAction action : request.getActionsList()) {
      if (action.hasDelete()) context.delete();
    }
    return Response.getDefaultInstance();
  }
}
