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

package kalix.scalasdk.action

import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.action.MessageEnvelopeImpl

object MessageEnvelope {

  /**
   * Create a message.
   *
   * @param payload
   *   The payload of the message.
   * @return
   *   The message.
   */
  def apply[T](payload: T): MessageEnvelope[T] = new MessageEnvelopeImpl[T](payload, Metadata.empty)

  /**
   * Create a message.
   *
   * @param payload
   *   The payload of the message.
   * @param metadata
   *   The metadata associated with the message.
   * @return
   *   The message.
   */
  def apply[T](payload: T, metadata: Metadata): MessageEnvelope[T] = new MessageEnvelopeImpl[T](payload, metadata)
}
trait MessageEnvelope[T] {

  /**
   * The metadata associated with the message.
   *
   * @return
   *   The metadata.
   */
  def metadata: Metadata

  /**
   * The payload of the message.
   *
   * @return
   *   The payload.
   */
  def payload: T

}
