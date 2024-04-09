/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.workflow

import kalix.scalasdk.MetadataContext

/** A value based workflow command context. */
trait CommandContext extends WorkflowContext with MetadataContext {

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
