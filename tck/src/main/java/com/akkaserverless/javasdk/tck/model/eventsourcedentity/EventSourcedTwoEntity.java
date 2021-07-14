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

import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase;
import com.akkaserverless.tck.model.EventSourcedEntity.Persisted;
import com.akkaserverless.tck.model.EventSourcedEntity.Request;
import com.akkaserverless.tck.model.EventSourcedEntity.Response;

@EventSourcedEntity(entityType = "EventSourcedTwoEntity")
public class EventSourcedTwoEntity extends EventSourcedEntityBase<Persisted> {

  @Override
  public Persisted emptyState() {
    return Persisted.getDefaultInstance();
  }

  @CommandHandler
  public EventSourcedEntityBase.Effect<Response> call(Persisted currentState, Request request) {
    return effects().reply(Response.getDefaultInstance());
  }
}
