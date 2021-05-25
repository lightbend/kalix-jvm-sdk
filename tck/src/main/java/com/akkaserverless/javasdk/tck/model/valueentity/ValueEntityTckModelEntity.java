/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    return reply.addEffects(e);
  }

  private ServiceCall serviceTwoRequest(String id) {
    return serviceTwoCall.createCall(Request.newBuilder().setId(id).build());
  }
}
