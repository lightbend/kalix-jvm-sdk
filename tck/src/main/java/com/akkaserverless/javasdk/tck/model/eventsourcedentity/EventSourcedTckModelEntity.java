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

package com.akkaserverless.javasdk.tck.model.eventsourcedentity;

import com.akkaserverless.javasdk.Context;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.eventsourcedentity.*;
import com.akkaserverless.tck.model.EventSourcedTwo;
import com.akkaserverless.tck.model.EventSourcedEntity.*;

import java.util.ArrayList;
import java.util.List;

@EventSourcedEntity(entityType = "event-sourced-tck-model", snapshotEvery = 5)
public class EventSourcedTckModelEntity {

  private final ServiceCallRef<Request> serviceTwoCall;

  private String state = "";

  public EventSourcedTckModelEntity(Context context) {
    serviceTwoCall =
        context.serviceCallFactory().lookup(EventSourcedTwo.name, "Call", Request.class);
  }

  @Snapshot
  public Persisted snapshot() {
    return Persisted.newBuilder().setValue(state).build();
  }

  @SnapshotHandler
  public void handleSnapshot(Persisted snapshot) {
    state = snapshot.getValue();
  }

  @EventHandler
  public void handleEvent(Persisted event) {
    state += event.getValue();
  }

  @CommandHandler
  public Reply<Response> process(Request request, CommandContext context) {
    Reply<Response> reply = null;
    List<com.akkaserverless.javasdk.Effect> e = new ArrayList<>();
    for (RequestAction action : request.getActionsList()) {
      switch (action.getActionCase()) {
        case EMIT:
          context.emit(Persisted.newBuilder().setValue(action.getEmit().getValue()).build());
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
