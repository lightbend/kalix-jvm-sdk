/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.eventsourcedentity

import kalix.scalasdk.MetadataContext

/** An event sourced command context. */
trait CommandContext extends EventSourcedEntityContext with MetadataContext {

  /**
   * The current sequence number of events in this entity.
   *
   * @return
   *   The current sequence number.
   */
  def sequenceNumber: Long

  /**
   * The name of the command being executed.
   *
   * @return
   *   The name of the command.
   */
  def commandName: String

  /**
   * The id of the command being executed.
   *
   * @return
   *   The id of the command.
   */
  def commandId: Long
}
