/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.eventsourcedentity;

/** Context for an event. */
public interface EventContext extends EventSourcedEntityContext {
  /**
   * The sequence number of the current event being processed.
   *
   * @return The sequence number.
   */
  long sequenceNumber();
}
