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

package com.example.wiring.eventsourcedentities.counter;

import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.javasdk.SideEffect;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import kalix.spring.KalixConfigurationTest;
import org.springframework.context.annotation.Import;

import java.util.concurrent.CompletionStage;

@Import(KalixConfigurationTest.class)
public class FalingForwardAction extends Action {

  private ComponentClient componentClient;

  private ActionCreationContext context;

  public FalingForwardAction(ComponentClient componentClient, ActionCreationContext context) {
    this.componentClient = componentClient;
    this.context = context;
  }

  @Subscribe.EventSourcedEntity(value = CounterEntity.class)
  public Effect<String> printIncrease(CounterEvent.ValueIncreased event) {
    System.out.println("testing" + event);
//    return effects().error("asd");
    return effects().forward(componentClient.forValueEntity("1").call(FailingEntity::doSth));
  }
}
