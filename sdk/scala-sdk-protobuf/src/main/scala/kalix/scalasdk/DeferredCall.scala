/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import scala.concurrent.Future

/**
 * Represents a call to a component service that has not yet happened, but will be handed to Kalix for execution. Used
 * with forwards and side effects.
 *
 * @tparam I
 *   the message type of the parameter for the call
 * @tparam O
 *   the message type that the call returns
 *
 * Not for user extension.
 */
trait DeferredCall[I, O] {

  /**
   * The message to pass to the call when the call is invoked.
   */
  def message: I

  /**
   * The metadata to pass with the message when the call is invoked.
   */
  def metadata: Metadata

  /**
   * Execute this call right away and get the async result back for composition. Can be used to create an async reply in
   * an [[kalix.scalasdk.action.Action]] using {{{effects.asyncReply}}} and {{{effects.asyncEffect}}}.
   */
  def execute(): Future[O]

  /**
   * Update with given metadata
   */
  def withMetadata(metadata: Metadata): DeferredCall[I, O]
}
