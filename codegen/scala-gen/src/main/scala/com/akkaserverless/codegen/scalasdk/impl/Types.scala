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

import com.lightbend.akkasls.codegen.ClassMessageType

object Types {
  object Action {
    val Action = ClassMessageType("com.akkaserverless.scalasdk.action.Action")
    val ActionCreationContext = ClassMessageType("com.akkaserverless.scalasdk.action.ActionCreationContext")
    val ActionOptions = ClassMessageType("com.akkaserverless.scalasdk.action.ActionOptions")
    val ActionProvider = ClassMessageType("com.akkaserverless.scalasdk.action.ActionProvider")
    val MessageEnvelope = ClassMessageType("com.akkaserverless.scalasdk.action.MessageEnvelope")
    val ActionRouter = ClassMessageType("com.akkaserverless.scalasdk.impl.action.ActionRouter")
    val HandlerNotFound = ClassMessageType("com.akkaserverless.javasdk.impl.action.ActionRouter.HandlerNotFound")
  }

  object EventSourcedEntity {
    val EventSourcedEntity = ClassMessageType("com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity")
    val EventSourcedEntityProvider = ClassMessageType(
      "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider")
    val EventSourcedEntityOptions = ClassMessageType(
      "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityOptions")
    val EventSourcedEntityContext = ClassMessageType(
      "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext")

    val CommandContext = ClassMessageType("com.akkaserverless.scalasdk.eventsourcedentity.CommandContext")
    val EventSourcedEntityRouter = ClassMessageType(
      "com.akkaserverless.scalasdk.impl.eventsourcedentity.EventSourcedEntityRouter")

    val CommandHandlerNotFound = ClassMessageType(
      "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.CommandHandlerNotFound")
    val EventHandlerNotFound = ClassMessageType(
      "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.EventHandlerNotFound")
  }

  object ValueEntity {
    val ValueEntity = ClassMessageType("com.akkaserverless.scalasdk.valueentity.ValueEntity")
    val CommandContext = ClassMessageType("com.akkaserverless.scalasdk.valueentity.CommandContext")
    val ValueEntityContext = ClassMessageType("com.akkaserverless.scalasdk.valueentity.ValueEntityContext")
    val ValueEntityOptions = ClassMessageType("com.akkaserverless.scalasdk.valueentity.ValueEntityOptions")
    val ValueEntityProvider = ClassMessageType("com.akkaserverless.scalasdk.valueentity.ValueEntityProvider")
    val ValueEntityRouter = ClassMessageType("com.akkaserverless.scalasdk.impl.valueentity.ValueEntityRouter")
    val CommandHandlerNotFound = ClassMessageType(
      "com.akkaserverless.javasdk.impl.valueentity.ValueEntityRouter.CommandHandlerNotFound")
  }

  val DeferredCall = ClassMessageType("com.akkaserverless.scalasdk.DeferredCall")
  val ScalaDeferredCallAdapter = ClassMessageType("com.akkaserverless.scalasdk.impl.ScalaDeferredCallAdapter")
  val InternalContext = ClassMessageType("com.akkaserverless.scalasdk.impl.InternalContext")
  val Context = ClassMessageType("com.akkaserverless.scalasdk.Context")
  val Metadata = ClassMessageType("com.akkaserverless.scalasdk.Metadata")

  val Source = ClassMessageType("akka.stream.scaladsl.Source")
  val NotUsed = ClassMessageType("akka.NotUsed")
  val ImmutableSeq = ClassMessageType("scala.collection.immutable.Seq")

  val Descriptors = ClassMessageType("com.google.protobuf.Descriptors")

}
