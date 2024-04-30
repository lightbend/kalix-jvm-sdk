/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.eventsourcedentity;

import kalix.javasdk.eventsourcedentity.*;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.tck.model.eventsourcedentity.EventSourcedEntityApi.*;

public class EventSourcedConfiguredEntity extends AbstractEventSourcedConfiguredEntity {

  public EventSourcedConfiguredEntity(EventSourcedEntityContext context) {}

  @Override
  public Persisted emptyState() {
    return Persisted.getDefaultInstance();
  }

  public EventSourcedEntity.Effect<Response> call(Persisted currentState, Request request) {
    return effects().reply(Response.getDefaultInstance());
  }

  @Override
  public Persisted persisted(Persisted currentState, Persisted persisted) {
    return currentState;
  }
}
