/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.JsonSupport;
import kalix.javasdk.eventsourcedentity.*;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Empty;

public class EventSourcedEntityTwo extends EventSourcedEntity<String, Object> {

  public EventSourcedEntityTwo(EventSourcedEntityContext context) {}

  @Override
  public String emptyState() {
    return "";
  }

  public EventSourcedEntity.Effect<Empty> emitJsonEvent(
      String currentState, LocalPersistenceEventing.JsonEvent event) {
    return effects()
        // FIXME requirement to use JSON events should be removed from TCK
        .emitEvent(JsonSupport.encodeJson(new JsonMessage(event.getMessage())))
        .thenReply(__ -> Empty.getDefaultInstance());
  }

  public String handle(String currentState, JsonMessage message) {
    return currentState;
  }
}
