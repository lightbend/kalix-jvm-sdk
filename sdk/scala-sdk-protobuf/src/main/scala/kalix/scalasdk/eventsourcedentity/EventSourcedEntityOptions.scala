/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.eventsourcedentity

import kalix.scalasdk.EntityOptions

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

  override def withForwardHeaders(headers: Set[String]): EventSourcedEntityOptions
}

object EventSourcedEntityOptions {

  /**
   * Create a default entity option for an event sourced entity.
   *
   * @return
   *   the entity option
   */
  def defaults: EventSourcedEntityOptions =
    EventSourcedEntityOptionsImpl(0, Set.empty)

  private[kalix] final case class EventSourcedEntityOptionsImpl(
      override val snapshotEvery: Int,
      override val forwardHeaders: Set[String])
      extends EventSourcedEntityOptions {

    override def withSnapshotEvery(numberOfEvents: Int): EventSourcedEntityOptions =
      copy(snapshotEvery = numberOfEvents)

    override def withForwardHeaders(headers: Set[String]): EventSourcedEntityOptions =
      copy(forwardHeaders = headers)
  }
}
