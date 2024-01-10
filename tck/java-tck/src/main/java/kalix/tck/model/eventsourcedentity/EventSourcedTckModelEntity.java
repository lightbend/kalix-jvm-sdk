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

package kalix.tck.model.eventsourcedentity;

import kalix.javasdk.Context;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.SideEffect;
import kalix.javasdk.eventsourcedentity.*;
import kalix.tck.model.eventsourcedentity.EventSourcedEntityApi.*;

import java.util.ArrayList;
import java.util.List;

public class EventSourcedTckModelEntity extends AbstractEventSourcedTckModelEntity {

  @Override
  public Persisted emptyState() {
    return Persisted.getDefaultInstance();
  }

  public EventSourcedTckModelEntity(Context context) {}

  public Persisted persisted(Persisted state, Persisted event) {
    return Persisted.newBuilder().setValue(state.getValue() + event.getValue()).build();
  }

  public EventSourcedEntity.Effect<Response> process(Persisted currentState, Request request) {
    List<SideEffect> e = new ArrayList<>();
    List<Persisted> events = new ArrayList<>();
    EventSourcedEntity.Effect<Response> effect = null;
    // TCK tests expect the logic to be imperative, building something up and then discarding on
    // failure but effect
    // api is not imperative so we have to keep track of failure instead
    boolean failed = false;
    for (RequestAction action : request.getActionsList()) {
      switch (action.getActionCase()) {
        case EMIT:
          if (!failed) {
            events.add(Persisted.newBuilder().setValue(action.getEmit().getValue()).build());
            effect =
                effects()
                    .emitEvents(events)
                    .thenReply(state -> Response.newBuilder().setMessage(state.getValue()).build());
          }
          break;
        case FORWARD:
          if (!failed) {
            // FIXME cannot be done in `thenForward` because components use command context and that
            // is cleared by then
            DeferredCall<Request, Response> forwardToServiceTwo =
                serviceTwoRequest(action.getForward().getId());
            effect = effects().emitEvents(events).thenForward(__ -> forwardToServiceTwo);
          }
          break;
        case EFFECT:
          if (!failed) {
            EventSourcedEntityApi.Effect actionEffect = action.getEffect();
            e.add(
                SideEffect.of(
                    serviceTwoRequest(actionEffect.getId()), actionEffect.getSynchronous()));
          }
          break;
        case FAIL:
          failed = true;
          effect = effects().error(action.getFail().getMessage());
          break;
      }
    }
    if (effect == null) {
      // then reply rather?
      effect = effects().reply(Response.newBuilder().setMessage(currentState.getValue()).build());
    }
    return effect.addSideEffects(e);
  }

  private DeferredCall<Request, Response> serviceTwoRequest(String id) {
    return components().eventSourcedTwoEntity().call(Request.newBuilder().setId(id).build());
  }
}
