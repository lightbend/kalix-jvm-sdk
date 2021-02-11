/*
 * Copyright 2019 Lightbend Inc.
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

package com.akkaserverless.javasdk.tck.model.eventlogeventing;

import com.akkaserverless.javasdk.eventsourced.CommandContext;
import com.akkaserverless.javasdk.eventsourced.CommandHandler;
import com.akkaserverless.javasdk.eventsourced.EventHandler;
import com.akkaserverless.javasdk.eventsourced.EventSourcedEntity;
import com.akkaserverless.tck.model.Eventlogeventing;
import com.google.protobuf.Empty;

@EventSourcedEntity(persistenceId = "eventlogeventing-one")
public class EventSourcedEntityOne {
  @CommandHandler
  public Empty emitEvent(Eventlogeventing.EmitEventRequest event, CommandContext ctx) {
    if (event.hasEventOne()) {
      ctx.emit(event.getEventOne());
    } else {
      ctx.emit(event.getEventTwo());
    }
    return Empty.getDefaultInstance();
  }

  @EventHandler
  public void handle(Eventlogeventing.EventOne event) {}

  @EventHandler
  public void handle(Eventlogeventing.EventTwo event) {}
}
