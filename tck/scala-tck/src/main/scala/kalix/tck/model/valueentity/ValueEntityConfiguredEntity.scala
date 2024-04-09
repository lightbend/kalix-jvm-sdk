/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.valueentity

import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext

/** A value entity. */
class ValueEntityConfiguredEntity(context: ValueEntityContext) extends AbstractValueEntityConfiguredEntity {
  override def emptyState: Persisted = Persisted.defaultInstance

  override def call(currentState: Persisted, request: Request): ValueEntity.Effect[Response] =
    effects.reply(Response.defaultInstance)
}
