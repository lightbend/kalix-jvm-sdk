/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.valueentity;

import com.akkaserverless.javasdk.Context;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.tck.model.ValueEntity.*;

import java.util.ArrayList;
import java.util.List;

@ValueEntity(entityType = "value-entity-tck-model")
public class ValueEntityTckModelEntity {

  private final ServiceCallRef<Request> serviceTwoCall;

  private String state = "";

  public ValueEntityTckModelEntity(Context context) {
    serviceTwoCall =
        context
            .serviceCallFactory()
            .lookup("akkaserverless.tck.model.valueentity.ValueEntityTwo", "Call", Request.class);
  }

  @CommandHandler
  public Reply<Response> process(Request request, CommandContext<Persisted> context) {
    Reply<Response> reply = null;
    List<com.akkaserverless.javasdk.Effect> e = new ArrayList<>();
    for (RequestAction action : request.getActionsList()) {
      switch (action.getActionCase()) {
        case UPDATE:
          state = action.getUpdate().getValue();
          context.updateState(Persisted.newBuilder().setValue(state).build());
          break;
        case DELETE:
          context.deleteState();
          state = "";
          break;
        case FORWARD:
          reply = Reply.forward(serviceTwoRequest(action.getForward().getId()));
          break;
        case EFFECT:
          Effect effect = action.getEffect();
          e.add(
              com.akkaserverless.javasdk.Effect.of(
                  serviceTwoRequest(effect.getId()), effect.getSynchronous()));
          break;
        case FAIL:
          reply = Reply.failure(action.getFail().getMessage());
          break;
      }
    }
    if (reply == null) {
      reply = Reply.message(Response.newBuilder().setMessage(state).build());
    }
    return reply.withEffects(e);
  }

  private ServiceCall serviceTwoRequest(String id) {
    return serviceTwoCall.createCall(Request.newBuilder().setId(id).build());
  }
}
