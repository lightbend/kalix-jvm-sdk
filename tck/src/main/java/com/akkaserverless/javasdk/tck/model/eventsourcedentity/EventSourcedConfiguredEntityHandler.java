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

import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase;
import com.akkaserverless.javasdk.impl.EntityExceptions;
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityHandler;
import com.akkaserverless.tck.model.EventSourcedEntity.Persisted;
import com.akkaserverless.tck.model.EventSourcedEntity.Request;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

/** An event sourced entity handler */
public class EventSourcedConfiguredEntityHandler
    extends EventSourcedEntityHandler<Persisted, EventSourcedConfiguredEntity> {

  public EventSourcedConfiguredEntityHandler(EventSourcedConfiguredEntity entity) {
    super(entity);
  }

  @Override
  public Persisted handleEvent(Persisted state, Object event) {
    throw new IllegalArgumentException("Unknown event type [" + event.getClass() + "]");
  }

  @Override
  public EventSourcedEntityBase.Effect<?> handleCommand(
      String commandName, Persisted state, Any command, CommandContext context) {
    try {
      switch (commandName) {
        case "Call":
          // FIXME could parsing to the right type also be pulled out of here?
          return entity().call(state, Request.parseFrom(command.getValue()));

        default:
          throw new EntityExceptions.EntityException(
              context.entityId(),
              context.commandId(),
              commandName,
              "No command handler found for command ["
                  + commandName
                  + "] on "
                  + entity().getClass());
      }
    } catch (InvalidProtocolBufferException ex) {
      // This is if command payload cannot be parsed
      throw new RuntimeException(ex);
    }
  }
}
