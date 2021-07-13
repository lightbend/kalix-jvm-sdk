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
import com.akkaserverless.javasdk.SideEffect;
import com.akkaserverless.javasdk.eventsourcedentity.*;
import com.akkaserverless.tck.model.EventSourcedTwo;
import com.akkaserverless.tck.model.EventSourcedEntity.*;

import java.util.ArrayList;
import java.util.List;

@EventSourcedEntity(entityType = "event-sourced-tck-model", snapshotEvery = 5)
public class EventSourcedTckModelEntity extends EventSourcedEntityBase<String> {

  private final ServiceCallRef<Request> serviceTwoCall;

  @Override
  public String emptyState() {
    return "";
  }

  public EventSourcedTckModelEntity(Context context) {
    serviceTwoCall =
        context.serviceCallFactory().lookup(EventSourcedTwo.name, "Call", Request.class);
  }

  @EventHandler
  public String handleEvent(String state, Persisted event) {
    return state + event.getValue();
  }

  @CommandHandler
  public Effect<Response> process(String currentState, Request request) {
    List<SideEffect> e = new ArrayList<>();
    Effect<Response> effect = null;
    for (RequestAction action : request.getActionsList()) {
      switch (action.getActionCase()) {
        case EMIT:
          effect =
              effects()
                  .emitEvent(Persisted.newBuilder().setValue(action.getEmit().getValue()).build())
                  .thenNoReply();
          break;
        case FORWARD:
          effect = effects().forward(serviceTwoRequest(action.getForward().getId()));
          break;
        case EFFECT:
          com.akkaserverless.tck.model.EventSourcedEntity.Effect actionEffect = action.getEffect();
          e.add(
              SideEffect.of(
                  serviceTwoRequest(actionEffect.getId()), actionEffect.getSynchronous()));
          break;
        case FAIL:
          effect = effects().error(action.getFail().getMessage());
          break;
      }
    }
    if (effect == null) {
      // then reply rather?
      effect = effects().reply(Response.newBuilder().setMessage(currentState).build());
    }
    return effect.addSideEffects(e);
  }

  private ServiceCall serviceTwoRequest(String id) {
    return serviceTwoCall.createCall(Request.newBuilder().setId(id).build());
  }
}
