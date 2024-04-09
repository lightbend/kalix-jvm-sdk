/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.eventsourcedentity

trait EventContext extends EventSourcedEntityContext {
  def sequenceNumber: Long
}
