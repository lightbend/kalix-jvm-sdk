/*
 * Copyright 2021 Lightbend Inc.
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

package com.akkaserverless.codegen.scalasdk.impl

import com.lightbend.akkasls.codegen.FullyQualifiedName

object Types {
  object Action {
    val Action = FQN("com.akkaserverless.scalasdk.action.Action")
    val ActionCreationContext = FQN("com.akkaserverless.scalasdk.action.ActionCreationContext")
    val ActionOptions = FQN("com.akkaserverless.scalasdk.action.ActionOptions")
    val ActionProvider = FQN("com.akkaserverless.scalasdk.action.ActionProvider")
    val MessageEnvelope = FQN("com.akkaserverless.scalasdk.action.MessageEnvelope")
    val ActionRouter = FQN("com.akkaserverless.scalasdk.impl.action.ActionRouter")
    val HandlerNotFound = FQN("com.akkaserverless.javasdk.impl.action.ActionRouter.HandlerNotFound")
  }

  object EventSourcedEntity {
    val EventSourcedEntity = FQN("com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity")
    val EventSourcedEntityProvider = FQN("com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider")
    val EventSourcedEntityOptions = FQN("com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityOptions")
    val EventSourcedEntityContext = FQN("com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext")

    val CommandContext = FQN("com.akkaserverless.scalasdk.eventsourcedentity.CommandContext")
    val EventSourcedEntityRouter = FQN("com.akkaserverless.scalasdk.impl.eventsourcedentity.EventSourcedEntityRouter")

    val CommandHandlerNotFound = FQN(
      "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.CommandHandlerNotFound")
    val EventHandlerNotFound = FQN(
      "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.EventHandlerNotFound")
  }

  object ValueEntity {
    val ValueEntity = FQN("com.akkaserverless.scalasdk.valueentity.ValueEntity")
    val CommandContext = FQN("com.akkaserverless.scalasdk.valueentity.CommandContext")
    val ValueEntityContext = FQN("com.akkaserverless.scalasdk.valueentity.ValueEntityContext")
    val ValueEntityOptions = FQN("com.akkaserverless.scalasdk.valueentity.ValueEntityOptions")
    val ValueEntityProvider = FQN("com.akkaserverless.scalasdk.valueentity.ValueEntityProvider")
    val ValueEntityRouter = FQN("com.akkaserverless.scalasdk.impl.valueentity.ValueEntityRouter")
    val CommandHandlerNotFound = FQN(
      "com.akkaserverless.javasdk.impl.valueentity.ValueEntityRouter.CommandHandlerNotFound")
  }

  val DeferredCall = FQN("com.akkaserverless.scalasdk.DeferredCall")
  val ScalaDeferredCallAdapter = FQN("com.akkaserverless.scalasdk.impl.ScalaDeferredCallAdapter")
  val InternalContext = FQN("com.akkaserverless.scalasdk.impl.InternalContext")
  val Context = FQN("com.akkaserverless.scalasdk.Context")
  val Metadata = FQN("com.akkaserverless.scalasdk.Metadata")

  val Source = FQN("akka.stream.scaladsl.Source")
  val NotUsed = FQN("akka.NotUsed")
  val ImmutableSeq = FQN("scala.collection.immutable.Seq")

  val Descriptors = FQN("com.google.protobuf.Descriptors")

  private def FQN(fqn: String): FullyQualifiedName =
    FullyQualifiedName.noDescriptor(fqn)
}
