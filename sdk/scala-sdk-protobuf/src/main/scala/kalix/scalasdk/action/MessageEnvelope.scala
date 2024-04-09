/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
