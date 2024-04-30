/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.eventsourcedentity

import kalix.scalasdk.eventsourcedentity.EventSourcedEntity

class EventSourcedConfiguredEntity extends AbstractEventSourcedConfiguredEntity {
  override def emptyState: Persisted = Persisted.defaultInstance

  override def call(currentState: Persisted, request: Request): EventSourcedEntity.Effect[Response] =
    effects.reply(Response.defaultInstance)

  override def persisted(currentState: Persisted, persisted: Persisted): Persisted =
    Persisted.defaultInstance
}
