/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.eventsourcedentity

import kalix.scalasdk.MetadataContext

trait EventContext extends EventSourcedEntityContext with MetadataContext {
  def sequenceNumber: Long
}
