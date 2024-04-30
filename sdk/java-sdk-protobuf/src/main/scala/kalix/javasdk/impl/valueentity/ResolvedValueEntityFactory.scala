/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.valueentity

import kalix.javasdk.impl.ResolvedEntityFactory
import kalix.javasdk.impl.ResolvedServiceMethod
import kalix.javasdk.impl.ValueEntityFactory
import kalix.javasdk.valueentity.ValueEntityContext

class ResolvedValueEntityFactory(
    delegate: ValueEntityFactory,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]])
    extends ValueEntityFactory
    with ResolvedEntityFactory {

  override def create(context: ValueEntityContext): ValueEntityRouter[_, _] =
    delegate.create(context)
}
