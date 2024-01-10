/*
 * Copyright 2024 Lightbend Inc.
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

  /**
   * @deprecated passivation strategy is ignored
   */
  @Override
  @Deprecated(since = "1.1.4", forRemoval = true)
  EventSourcedEntityOptions withPassivationStrategy(PassivationStrategy strategy);

  @Override
  EventSourcedEntityOptions withForwardHeaders(Set<String> headers);

  /**
   * Create a default entity option for an event sourced entity.
   *
   * @return the entity option
   */
  static EventSourcedEntityOptions defaults() {
    return new EventSourcedEntityOptionsImpl(
        0, PassivationStrategy.defaultTimeout(), Collections.emptySet());
  }
}
