/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.action

import kalix.scalasdk.Metadata
import kalix.scalasdk.action.MessageEnvelope

private[scalasdk] final case class MessageEnvelopeImpl[T](payload: T, metadata: Metadata) extends MessageEnvelope[T]
