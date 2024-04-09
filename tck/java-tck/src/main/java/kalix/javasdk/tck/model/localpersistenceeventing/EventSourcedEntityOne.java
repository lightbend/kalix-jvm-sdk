/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.eventsourcedentity.*;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Empty;

public class EventSourcedEntityOne extends EventSourcedEntity<String, Object> {

  public EventSourcedEntityOne(EventSourcedEntityContext context) {}

  @Override
  public String emptyState() {
    return "";
  }

  public Effect<Empty> emitEvent(
      String currentState, LocalPersistenceEventing.EmitEventRequest event) {
    if (event.hasEventOne()) {
      return effects().emitEvent(event.getEventOne()).thenReply(__ -> Empty.getDefaultInstance());
    } else {
      return effects().emitEvent(event.getEventTwo()).thenReply(__ -> Empty.getDefaultInstance());
    }
  }

  public String handle(String currentState, LocalPersistenceEventing.EventOne event) {
    return currentState;
  }

  public String handle(String currentState, LocalPersistenceEventing.EventTwo event) {
    return currentState;
  }
}
