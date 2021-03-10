/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.tck.model.valueentity;

import com.akkaserverless.javasdk.Context;
import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.tck.model.valueentity.Valueentity.*;

import java.util.Optional;

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
  public Optional<Response> process(Request request, CommandContext<Persisted> context) {
    boolean forwarding = false;
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
          forwarding = true;
          context.forward(serviceTwoRequest(action.getForward().getId()));
          break;
        case EFFECT:
          Effect effect = action.getEffect();
          context.effect(serviceTwoRequest(effect.getId()), effect.getSynchronous());
          break;
        case FAIL:
          context.fail(action.getFail().getMessage());
          break;
      }
    }
    return forwarding
        ? Optional.empty()
        : Optional.of(Response.newBuilder().setMessage(state).build());
  }

  private ServiceCall serviceTwoRequest(String id) {
    return serviceTwoCall.createCall(Request.newBuilder().setId(id).build());
  }
}
