/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.workflow

import kalix.javasdk.impl.ResolvedEntityFactory
import kalix.javasdk.impl.ResolvedServiceMethod
import kalix.javasdk.impl.WorkflowFactory
import kalix.javasdk.workflow.WorkflowContext

class ResolvedWorkflowFactory(
    delegate: WorkflowFactory,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]])
    extends WorkflowFactory
    with ResolvedEntityFactory {

  override def create(context: WorkflowContext): WorkflowRouter[_, _] =
    delegate.create(context)
}
