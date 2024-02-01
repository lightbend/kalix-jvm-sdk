/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.scalasdk.workflow

import scala.concurrent.Future
import scala.reflect.ClassTag

import akka.annotation.ApiMayChange
import kalix.scalasdk.DeferredCall
import kalix.scalasdk.workflow.AbstractWorkflow.Effect
import scalapb.GeneratedMessage

object ProtoStepBuilder {

  /* callFactory builds the DeferredCall that will be passed to proxy for execution */
  case class CallStepBuilder[Input, DefCallInput, DefCallOutput](
      name: String,
      callInputClass: Class[Input],
      callFunc: Input => DeferredCall[DefCallInput, DefCallOutput]) {

    /**
     * Transition to the next step based on the result of the step call.
     *
     * The function passed to this method should receive the return type of the step call and return an
     * [[Workflow.Effect.TransitionalEffect]] describing the next step to transition to.
     *
     * When defining the Effect, you can update the workflow state and indicate the next step to transition to. This can
     * be another step, or a pause or end of the workflow. <p> When transition to another step, you can also pass an
     * input parameter to the next step.
     *
     * @param transitionFunc
     *   Function that transform the action result to a [[Workflow.Effect.TransitionalEffect]]
     * @return
     *   CallStep
     */
    @ApiMayChange
    def andThen(transitionFunc: Function[DefCallOutput, Effect.TransitionalEffect[Void]])(implicit
        transitionInputClassTag: ClassTag[DefCallOutput]) =
      AbstractWorkflow.CallStep[Input, DefCallInput, DefCallOutput, Any](
        name,
        callInputClass,
        callFunc,
        transitionInputClassTag.runtimeClass.asInstanceOf[Class[DefCallOutput]],
        transitionFunc)
  }

  class AsyncCallStepBuilder[CallInput, CallOutput](
      private val name: String,
      private val callInputClass: Class[CallInput],
      private val callFunc: CallInput => Future[CallOutput]) {

    /**
     * Transition to the next step based on the result of the step call.
     *
     * The function passed to this method should receive the return type of the step call and return an
     * [[Workflow.Effect.TransitionalEffect]] describing the next step to transition to.
     *
     * When defining the Effect, you can update the workflow state and indicate the next step to transition to. This can
     * be another step, or a pause or end of the workflow. <p> When transition to another step, you can also pass an
     * input parameter to the next step.
     *
     * @param transitionFunc
     *   Function that transform the action result to a [[Workflow.Effect.TransitionalEffect]]
     * @return
     *   AsyncCallStep
     */
    @ApiMayChange def andThen(transitionFunc: CallOutput => Effect.TransitionalEffect[Void])(implicit
        transitionInputClassTag: ClassTag[CallOutput]) =
      new AbstractWorkflow.AsyncCallStep[CallInput, CallOutput, AnyRef](
        name,
        callInputClass,
        callFunc,
        transitionInputClassTag.runtimeClass.asInstanceOf[Class[CallOutput]],
        transitionFunc)
  }
}

/**
 * Step builder for defining a workflow step for Scala protobuf SDK
 */
case class ProtoStepBuilder(name: String) {

  /**
   * Build a step action with a call to an existing Kalix component via [[kalix.scalasdk.DeferredCall]].
   *
   * The function passed to this method should return a [[kalix.scalasdk.DeferredCall]]. The
   * [[kalix.scalasdk.DeferredCall]] is then executed by Kalix and its result, if successful, is made available to this
   * workflow via the `andThen` method. In the `andThen` method, we can use the result to update the workflow state and
   * transition to the next step.
   *
   * On failure, the step will be retried according to the default retry strategy or the one defined in the step
   * configuration.
   *
   * @param callFactory
   *   Factory method for creating deferred call.
   * @tparam Input
   *   Input for deferred call factory, provided by transition method.
   * @tparam DefCallInput
   *   Input for deferred call.
   * @tparam DefCallOutput
   *   Output of deferred call.
   * @return
   *   Step builder.
   */
  def call[Input <: GeneratedMessage, DefCallInput, DefCallOutput <: GeneratedMessage](
      callFactory: Input => DeferredCall[DefCallInput, DefCallOutput])(implicit
      inputClassTag: scala.reflect.ClassTag[Input]) =
    ProtoStepBuilder.CallStepBuilder[Input, DefCallInput, DefCallOutput](
      name,
      inputClassTag.runtimeClass.asInstanceOf[Class[Input]],
      callFactory)

  /**
   * Build a step action with a call to an existing Kalix component via [[kalix.scalasdk.DeferredCall]].
   *
   * The function passed to this method should return a [[kalix.scalasdk.DeferredCall]]. The
   * [[kalix.scalasdk.DeferredCall]] is then executed by Kalix and its result, if successful, is made available to this
   * workflow via the `andThen` method. In the `andThen` method, we can use the result to update the workflow state and
   * transition to the next step.
   *
   * On failure, the step will be retried according to the default retry strategy or the one defined in the step
   * configuration.
   *
   * @param callSupplier
   *   Factory method for creating deferred call.
   * @tparam DefCallInput
   *   Input for deferred call.
   * @tparam DefCallOutput
   *   Output of deferred call.
   * @return
   *   Step builder.
   */
  @ApiMayChange
  def call[DefCallInput, DefCallOutput <: GeneratedMessage](
      callSupplier: () => DeferredCall[DefCallInput, DefCallOutput]) =
    new ProtoStepBuilder.CallStepBuilder[Void, DefCallInput, DefCallOutput](name, classOf[Void], _ => callSupplier())

  /**
   * Build a step action with an async call.
   *
   * The function passed to this method should return a [[Future]]. On successful completion, its result is made
   * available to this workflow via the `andThen` method. In the `andThen` method, we can use the result to update the
   * workflow state and transition to the next step.
   *
   * On failure, the step will be retried according to the default retry strategy or the one defined in the step
   * configuration.
   *
   * @param callFactory
   *   Factory method for creating async call.
   * @tparam Input
   *   Input for async call factory, provided by transition method.
   * @tparam Output
   *   Output of async call.
   * @return
   *   Step builder.
   */
  @ApiMayChange
  def asyncCall[Input <: GeneratedMessage, Output <: GeneratedMessage](callFactory: Input => Future[Output])(implicit
      callInputClassTag: scala.reflect.ClassTag[Input]) =
    new ProtoStepBuilder.AsyncCallStepBuilder[Input, Output](
      name,
      callInputClassTag.runtimeClass.asInstanceOf[Class[Input]],
      callFactory)

  /**
   * Build a step action with an async call.
   *
   * The function passed to this method should return a [[Future]]. On successful completion, its result is made
   * available to this workflow via the `andThen` method. In the `andThen` method, we can use the result to update the
   * workflow state and transition to the next step.
   *
   * On failure, the step will be retried according to the default retry strategy or the one defined in the step
   * configuration.
   *
   * @param callSupplier
   *   Factory method for creating async call.
   * @tparam Output
   *   Output of async call.
   * @return
   *   Step builder.
   */
  @ApiMayChange
  def asyncCall[Output <: GeneratedMessage](callSupplier: () => Future[Output]) =
    new ProtoStepBuilder.AsyncCallStepBuilder[Void, Output](name, classOf[Void], _ => callSupplier())

}
