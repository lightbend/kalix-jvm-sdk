/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.workflow

import akka.annotation.ApiMayChange
import scalapb.GeneratedMessage

/**
 * Workflows are stateful components and are defined by a set of steps and transitions between them.
 *
 * You can use workflows to implement business processes that span multiple services.
 *
 * When implementing a workflow, you define a state type and a set of steps. Each step defines a call to be executed and
 * the transition to the next step based on the result of the call. The workflow state can be updated after each
 * successful step execution.
 *
 * Kalix keeps track of the state of the workflow and the current step. If the workflow is stopped for any reason, it
 * can be resumed from the last known state and step.
 *
 * Workflow methods that handle incoming commands should return an [[AbstractWorkflow.Effect]] describing the next
 * processing actions.
 *
 * @tparam S
 *   The type of the state for this workflow.
 */
@ApiMayChange
abstract class ProtoWorkflow[S >: Null <: GeneratedMessage] extends AbstractWorkflow[S] {

  /**
   * Start a step definition with a given step name.
   *
   * @param name
   *   Step name.
   * @return
   *   Step builder.
   */
  def step(name: String) = new ProtoStepBuilder(name)
}
