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

package com.example.wiring.eventsourcedentities.counter;

import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.SideEffect;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;

import java.util.concurrent.CompletionStage;

public class IncreaseAction extends Action {

  private ComponentClient componentClient;

  private ActionCreationContext context;

  public IncreaseAction(ComponentClient componentClient, ActionCreationContext context) {
    this.componentClient = componentClient;
    this.context = context;
  }

  @Subscribe.EventSourcedEntity(value = CounterEntity.class)
  public Effect<CounterEvent.ValueMultiplied> printMultiply(CounterEvent.ValueMultiplied event) {
    return effects().reply(event);
  }

  @Subscribe.EventSourcedEntity(value = CounterEntity.class)
  public Effect<CounterEvent.ValueSet> printSet(CounterEvent.ValueSet event) {
    return effects().reply(event);
  }

  @Subscribe.EventSourcedEntity(value = CounterEntity.class)
  public Effect<Integer> printIncrease(CounterEvent.ValueIncreased event) {
    String entityId = this.actionContext().metadata().asCloudEvent().subject().get();
    if (event.value() == 42) {
      CompletionStage<Integer> res = componentClient.forEventSourcedEntity(entityId).call(CounterEntity::increase).params(1).execute();
      return effects().asyncReply(res);
    } else if (event.value() == 4422) {
      DeferredCall<Any, Integer> inc = componentClient.forEventSourcedEntity(entityId).call(CounterEntity::increase).params(1);
      return effects().reply(event.value())
          .addSideEffect(SideEffect.of(inc));
    }
    return effects().reply(event.value());
  }
}
