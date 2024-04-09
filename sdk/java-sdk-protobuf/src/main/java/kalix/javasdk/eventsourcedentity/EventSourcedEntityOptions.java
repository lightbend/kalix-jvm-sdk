/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.eventsourcedentity;

import kalix.javasdk.EntityOptions;
import kalix.javasdk.PassivationStrategy;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityOptionsImpl;

import java.util.Collections;
import java.util.Set;

/** Root entity options for all event sourcing entities. */
public interface EventSourcedEntityOptions extends EntityOptions {

  int snapshotEvery();

  /**
   * Specifies how snapshots of the entity state should be made: Zero means use default from
   * configuration file. Any negative value means never snapshot. Any positive value means snapshot
   * at-or-after that number of events.
   *
   * <p>It is strongly recommended to not disable snapshotting unless it is known that event sourced
   * entity will never have more than 100 events (in which case the default will anyway not trigger
   * any snapshots)
   */
  EventSourcedEntityOptions withSnapshotEvery(int numberOfEvents);

  @Override
  EventSourcedEntityOptions withForwardHeaders(Set<String> headers);

  /**
   * Create a default entity option for an event sourced entity.
   *
   * @return the entity option
   */
  static EventSourcedEntityOptions defaults() {
    return new EventSourcedEntityOptionsImpl(0, Collections.emptySet());
  }
}
