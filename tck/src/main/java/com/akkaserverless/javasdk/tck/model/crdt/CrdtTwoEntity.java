/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.crdt;

import com.akkaserverless.javasdk.crdt.CommandContext;
import com.akkaserverless.javasdk.crdt.CommandHandler;
import com.akkaserverless.javasdk.crdt.CrdtEntity;
import com.akkaserverless.javasdk.crdt.GCounter;
import com.akkaserverless.tck.model.Crdt.Request;
import com.akkaserverless.tck.model.Crdt.RequestAction;
import com.akkaserverless.tck.model.Crdt.Response;

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
