/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.valueentity

import kalix.scalasdk.MetadataContext

/** A value based entity command context. */
trait CommandContext extends ValueEntityContext with MetadataContext {

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
