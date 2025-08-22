/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.eventsourcedentity;

import kalix.javasdk.Metadata;

public final class EventWithMetadata<E> {
  private final E event;
  private final Metadata metadata;

  public EventWithMetadata(E event, Metadata metadata) {
    this.event = event;
    this.metadata = metadata;
  }

  public E getEvent() {
    return event;
  }

  public Metadata getMetadata() {
    return metadata;
  }
}
