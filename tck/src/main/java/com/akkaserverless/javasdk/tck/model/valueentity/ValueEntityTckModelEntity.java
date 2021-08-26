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
import com.akkaserverless.javasdk.ServiceCall;
import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.SideEffect;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.tck.model.ValueEntity.Persisted;
import com.akkaserverless.tck.model.ValueEntity.Request;
import com.akkaserverless.tck.model.ValueEntity.RequestAction;
import com.akkaserverless.tck.model.ValueEntity.Response;

import java.util.ArrayList;
import java.util.List;

public class ValueEntityTckModelEntity extends ValueEntity<Persisted> {

  private final ServiceCallRef<Request> serviceTwoCall;

  public ValueEntityTckModelEntity(Context context) {
    serviceTwoCall =
        context
            .serviceCallFactory()
            .lookup("akkaserverless.tck.model.valueentity.ValueEntityTwo", "Call", Request.class);
  }

  public Effect<Response> process(Persisted state, Request request) {
    // FIXME the effect API doesn't support all combinations, and that might be fine?
    String value = state.getValue();
    Effect.OnSuccessBuilder<Persisted> builder = null;
    Effect<Response> result = null;
    List<SideEffect> e = new ArrayList<>();
    for (RequestAction action : request.getActionsList()) {
      switch (action.getActionCase()) {
        case UPDATE:
          value = action.getUpdate().getValue();
          builder = effects().updateState(Persisted.newBuilder().setValue(value).build());
          break;
        case DELETE:
          value = "";
          builder = effects().deleteState();
          break;
        case FORWARD:
          if (builder == null) {
            result = effects().forward(serviceTwoRequest(action.getForward().getId()));
          } else {
            result = builder.thenForward(serviceTwoRequest(action.getForward().getId()));
          }
          break;
        case EFFECT:
          com.akkaserverless.tck.model.ValueEntity.Effect effect = action.getEffect();
          e.add(SideEffect.of(serviceTwoRequest(effect.getId()), effect.getSynchronous()));
          break;
        case FAIL:
          result = effects().error(action.getFail().getMessage());
          break;
      }
    }
    if (builder == null && result == null) {
      return effects().reply(Response.newBuilder().setMessage(value).build()).addSideEffects(e);
    }
    if (result == null) {
      return builder.thenReply(Response.newBuilder().setMessage(value).build()).addSideEffects(e);
    } else {
      return result.addSideEffects(e);
    }
  }

  private ServiceCall serviceTwoRequest(String id) {
    return serviceTwoCall.createCall(Request.newBuilder().setId(id).build());
  }

  @Override
  public Persisted emptyState() {
    return Persisted.getDefaultInstance();
  }
}
