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

package kalix.scalasdk.eventsourcedentity

import kalix.scalasdk.EntityOptions
import kalix.scalasdk.PassivationStrategy
import scala.collection.immutable.Set

import java.util.Collections

/** Root entity options for all event sourcing entities. */
trait EventSourcedEntityOptions extends EntityOptions {

  def snapshotEvery: Int

  /**
   * Specifies how snapshots of the entity state should be made: Zero means use default from configuration file. Any
   * negative value means never snapshot. Any positive value means snapshot at-or-after that number of events.
   *
   * <p>It is strongly recommended to not disable snapshotting unless it is known that event sourced entity will never
   * have more than 100 events (in which case the default will anyway not trigger any snapshots)
   */
  def withSnapshotEvery(numberOfEvents: Int): EventSourcedEntityOptions

  @deprecated(message = "passivation strategy is ignored", since = "1.1.4")
  override def withPassivationStrategy(strategy: PassivationStrategy): EventSourcedEntityOptions
  override def withForwardHeaders(headers: Set[String]): EventSourcedEntityOptions
}

object EventSourcedEntityOptions {

  /**
   * Create a default entity option for an event sourced entity.
   *
   * @return
   *   the entity option
   */
  def defaults: EventSourcedEntityOptions = {
    EventSourcedEntityOptionsImpl(0, PassivationStrategy.defaultTimeout, Set.empty)
  }

  private[kalix] final case class EventSourcedEntityOptionsImpl(
      override val snapshotEvery: Int,
      override val passivationStrategy: PassivationStrategy,
      override val forwardHeaders: Set[String])
      extends EventSourcedEntityOptions {

    override def withSnapshotEvery(numberOfEvents: Int): EventSourcedEntityOptions =
      copy(snapshotEvery = numberOfEvents)

    override def withPassivationStrategy(strategy: PassivationStrategy): EventSourcedEntityOptions =
      copy(passivationStrategy = strategy)

    override def withForwardHeaders(headers: Set[String]): EventSourcedEntityOptions =
      copy(forwardHeaders = headers)
  }
}
