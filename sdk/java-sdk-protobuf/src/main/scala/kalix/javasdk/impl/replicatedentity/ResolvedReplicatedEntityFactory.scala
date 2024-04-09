/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import kalix.javasdk.impl.ReplicatedEntityFactory
import kalix.javasdk.impl.ResolvedEntityFactory
import kalix.javasdk.impl.ResolvedServiceMethod
import kalix.javasdk.replicatedentity.ReplicatedEntityContext

class ResolvedReplicatedEntityFactory(
    delegate: ReplicatedEntityFactory,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]])
    extends ReplicatedEntityFactory
    with ResolvedEntityFactory {

  override def create(context: ReplicatedEntityContext): ReplicatedEntityRouter[_, _] =
    delegate.create(context)
}
