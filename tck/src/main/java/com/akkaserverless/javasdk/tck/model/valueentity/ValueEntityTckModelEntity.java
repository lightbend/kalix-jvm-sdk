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

import com.akkaserverless.javasdk.*;
import com.akkaserverless.javasdk.reply.MessageReply;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.javasdk.valueentity.ValueEntityBase;
import com.akkaserverless.tck.model.ValueEntity.Persisted;
import com.akkaserverless.tck.model.ValueEntity.Request;
import com.akkaserverless.tck.model.ValueEntity.Response;
import com.akkaserverless.tck.model.ValueEntity.RequestAction;

import java.util.ArrayList;
import java.util.List;

@ValueEntity(entityType = "value-entity-tck-model")
public class ValueEntityTckModelEntity extends ValueEntityBase<Persisted> {

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
    List<SideEffect> e = new ArrayList<>();
    String newState = state;
    for (RequestAction action : request.getActionsList()) {
      switch (action.getActionCase()) {
        case UPDATE:
          newState = action.getUpdate().getValue();
          context.updateState(Persisted.newBuilder().setValue(state).build());
          break;
        case DELETE:
          newState = "";
          break;
        case FORWARD:
          reply = effects().forward(serviceTwoRequest(action.getForward().getId()));
          break;
        case EFFECT:
          com.akkaserverless.tck.model.ValueEntity.Effect effect = action.getEffect();
          e.add(SideEffect.of(serviceTwoRequest(effect.getId()), effect.getSynchronous()));
          break;
        case FAIL:
          return effects().error(action.getFail().getMessage());
      }
    }

    effects().updateState(null).
  }

  private ServiceCall serviceTwoRequest(String id) {
    return serviceTwoCall.createCall(Request.newBuilder().setId(id).build());
  }

  @Override
  protected Persisted emptyState() {
    return Persisted.getDefaultInstance();
  }
}
