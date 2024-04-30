/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.valueentity

import kalix.scalasdk.valueentity.ValueEntity

/** A value entity. */
class ValueEntityTwoEntity extends AbstractValueEntityTwoEntity {
  override def emptyState: Persisted = Persisted.defaultInstance

  override def call(currentState: Persisted, request: Request): ValueEntity.Effect[Response] =
    effects.reply(Response.defaultInstance)
}
