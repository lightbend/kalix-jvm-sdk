/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.action

import kalix.javasdk.action.ActionCreationContext
import kalix.javasdk.impl.ActionFactory
import kalix.javasdk.impl.ResolvedEntityFactory
import kalix.javasdk.impl.ResolvedServiceMethod

class ResolvedActionFactory(
    delegate: ActionFactory,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]])
    extends ActionFactory
    with ResolvedEntityFactory {
  override def create(context: ActionCreationContext): ActionRouter[_] =
    delegate.create(context)

}
