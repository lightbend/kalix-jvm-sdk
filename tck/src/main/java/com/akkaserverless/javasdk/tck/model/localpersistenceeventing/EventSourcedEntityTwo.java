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

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import com.akkaserverless.javasdk.eventsourcedentity.*;
import com.akkaserverless.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Empty;

public class EventSourcedEntityTwo extends EventSourcedEntity<String> {

  public EventSourcedEntityTwo(EventSourcedEntityContext context) {}

  @Override
  public String emptyState() {
    return "";
  }

  public EventSourcedEntity.Effect<Empty> emitJsonEvent(
      String currentState, LocalPersistenceEventing.JsonEvent event) {
    return effects()
        .emitEvent(new JsonMessage(event.getMessage()))
        .thenReply(__ -> Empty.getDefaultInstance());
  }

  public String handle(String currentState, JsonMessage message) {
    return currentState;
  }
}
