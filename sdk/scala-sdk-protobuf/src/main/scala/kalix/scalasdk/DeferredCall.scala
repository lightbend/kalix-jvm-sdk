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
