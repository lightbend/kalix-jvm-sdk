/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.tck.model.valueentity;

import kalix.javasdk.Context;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.SideEffect;
import kalix.tck.model.valueentity.ValueEntityApi.*;

import java.util.ArrayList;
import java.util.List;

public class ValueEntityTckModelEntity extends AbstractValueEntityTckModelEntity {

  public ValueEntityTckModelEntity(Context context) {}

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
          builder = effects().deleteEntity();
          break;
        case FORWARD:
          if (builder == null) {
            result = effects().forward(serviceTwoRequest(action.getForward().getId()));
          } else {
            result = builder.thenForward(serviceTwoRequest(action.getForward().getId()));
          }
          break;
        case EFFECT:
          ValueEntityApi.Effect effect = action.getEffect();
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

  private DeferredCall<Request, Response> serviceTwoRequest(String id) {
    return components().valueEntityTwoEntity().call(Request.newBuilder().setId(id).build());
  }

  @Override
  public Persisted emptyState() {
    return Persisted.getDefaultInstance();
  }
}
