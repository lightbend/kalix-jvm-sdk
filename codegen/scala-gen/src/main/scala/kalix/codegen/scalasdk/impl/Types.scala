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

package kalix.codegen.scalasdk.impl

import kalix.codegen.ClassMessageType

object Types {
  object Action {
    val Action = ClassMessageType("kalix.scalasdk.action.Action")
    val ActionCreationContext = ClassMessageType("kalix.scalasdk.action.ActionCreationContext")
    val ActionOptions = ClassMessageType("kalix.scalasdk.action.ActionOptions")
    val ActionProvider = ClassMessageType("kalix.scalasdk.action.ActionProvider")
    val MessageEnvelope = ClassMessageType("kalix.scalasdk.action.MessageEnvelope")
    val ActionRouter = ClassMessageType("kalix.scalasdk.impl.action.ActionRouter")
    val HandlerNotFound = ClassMessageType("kalix.javasdk.impl.action.ActionRouter.HandlerNotFound")
  }

  object EventSourcedEntity {
    val EventSourcedEntity = ClassMessageType("kalix.scalasdk.eventsourcedentity.EventSourcedEntity")
    val EventSourcedEntityProvider = ClassMessageType("kalix.scalasdk.eventsourcedentity.EventSourcedEntityProvider")
    val EventSourcedEntityOptions = ClassMessageType("kalix.scalasdk.eventsourcedentity.EventSourcedEntityOptions")
    val EventSourcedEntityContext = ClassMessageType("kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext")

    val CommandContext = ClassMessageType("kalix.scalasdk.eventsourcedentity.CommandContext")
    val EventSourcedEntityRouter = ClassMessageType("kalix.scalasdk.impl.eventsourcedentity.EventSourcedEntityRouter")

    val CommandHandlerNotFound = ClassMessageType(
      "kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.CommandHandlerNotFound")
    val EventHandlerNotFound = ClassMessageType(
      "kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.EventHandlerNotFound")
  }

  object ValueEntity {
    val ValueEntity = ClassMessageType("kalix.scalasdk.valueentity.ValueEntity")
    val CommandContext = ClassMessageType("kalix.scalasdk.valueentity.CommandContext")
    val ValueEntityContext = ClassMessageType("kalix.scalasdk.valueentity.ValueEntityContext")
    val ValueEntityOptions = ClassMessageType("kalix.scalasdk.valueentity.ValueEntityOptions")
    val ValueEntityProvider = ClassMessageType("kalix.scalasdk.valueentity.ValueEntityProvider")
    val ValueEntityRouter = ClassMessageType("kalix.scalasdk.impl.valueentity.ValueEntityRouter")
    val CommandHandlerNotFound = ClassMessageType(
      "kalix.javasdk.impl.valueentity.ValueEntityRouter.CommandHandlerNotFound")
  }

  object Workflow {
    val AbstractWorkflow = ClassMessageType("kalix.scalasdk.workflow.AbstractWorkflow")
    val ProtoWorkflow = ClassMessageType("kalix.scalasdk.workflow.ProtoWorkflow")
    val CommandContext = ClassMessageType("kalix.scalasdk.workflow.CommandContext")
    val WorkflowContext = ClassMessageType("kalix.scalasdk.workflow.WorkflowContext")
    val WorkflowOptions = ClassMessageType("kalix.scalasdk.workflow.WorkflowOptions")
    val WorkflowProvider = ClassMessageType("kalix.scalasdk.workflow.WorkflowProvider")
    val WorkflowRouter = ClassMessageType("kalix.scalasdk.impl.workflow.WorkflowRouter")
    val CommandHandlerNotFound = ClassMessageType("kalix.javasdk.impl.workflow.WorkflowRouter.CommandHandlerNotFound")
  }

  val DeferredCall = ClassMessageType("kalix.scalasdk.DeferredCall")
  val ScalaDeferredCallAdapter = ClassMessageType("kalix.scalasdk.impl.ScalaDeferredCallAdapter")
  val InternalContext = ClassMessageType("kalix.scalasdk.impl.InternalContext")
  val Context = ClassMessageType("kalix.scalasdk.Context")
  val Metadata = ClassMessageType("kalix.scalasdk.Metadata")
  val SingleResponseRequestBuilder = ClassMessageType("akka.grpc.scaladsl.SingleResponseRequestBuilder")

  val Source = ClassMessageType("akka.stream.scaladsl.Source")
  val NotUsed = ClassMessageType("akka.NotUsed")
  val ImmutableSeq = ClassMessageType("scala.collection.immutable.Seq")

  val Descriptors = ClassMessageType("com.google.protobuf.Descriptors")

}
