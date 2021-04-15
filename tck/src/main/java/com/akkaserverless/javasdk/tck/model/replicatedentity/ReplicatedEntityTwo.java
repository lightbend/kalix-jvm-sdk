/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.replicatedentity;

import com.akkaserverless.javasdk.replicatedentity.CommandContext;
import com.akkaserverless.javasdk.replicatedentity.CommandHandler;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
import com.akkaserverless.javasdk.replicatedentity.GCounter;
import com.akkaserverless.tck.model.ReplicatedEntity.Request;
import com.akkaserverless.tck.model.ReplicatedEntity.RequestAction;
import com.akkaserverless.tck.model.ReplicatedEntity.Response;

@ReplicatedEntity
public class ReplicatedEntityTwo {
  // create replicated data to be able to call delete
  public ReplicatedEntityTwo(GCounter counter) {}

  @CommandHandler
  public Response call(Request request, CommandContext context) {
    for (RequestAction action : request.getActionsList()) {
      if (action.hasDelete()) context.delete();
    }
    return Response.getDefaultInstance();
  }
}
