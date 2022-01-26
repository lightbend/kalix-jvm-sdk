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

import com.lightbend.akkasls.codegen.PojoMessageType
import com.lightbend.akkasls.codegen.SourceGeneratorUtils.CodeBlock

object Types {
  object Action {
    val Action = PojoMessageType("com.akkaserverless.scalasdk.action.Action")
    val ActionCreationContext = PojoMessageType("com.akkaserverless.scalasdk.action.ActionCreationContext")
    val ActionOptions = PojoMessageType("com.akkaserverless.scalasdk.action.ActionOptions")
    val ActionProvider = PojoMessageType("com.akkaserverless.scalasdk.action.ActionProvider")
    val MessageEnvelope = PojoMessageType("com.akkaserverless.scalasdk.action.MessageEnvelope")
    val ActionRouter = PojoMessageType("com.akkaserverless.scalasdk.impl.action.ActionRouter")
    val HandlerNotFound = PojoMessageType("com.akkaserverless.javasdk.impl.action.ActionRouter.HandlerNotFound")
  }

  object EventSourcedEntity {
    val EventSourcedEntity = PojoMessageType("com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity")
    val EventSourcedEntityProvider = PojoMessageType(
      "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider")
    val EventSourcedEntityOptions = PojoMessageType(
      "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityOptions")
    val EventSourcedEntityContext = PojoMessageType(
      "com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext")

    val CommandContext = PojoMessageType("com.akkaserverless.scalasdk.eventsourcedentity.CommandContext")
    val EventSourcedEntityRouter = PojoMessageType(
      "com.akkaserverless.scalasdk.impl.eventsourcedentity.EventSourcedEntityRouter")

    val CommandHandlerNotFound = PojoMessageType(
      "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.CommandHandlerNotFound")
    val EventHandlerNotFound = PojoMessageType(
      "com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter.EventHandlerNotFound")
  }

  object ValueEntity {
    val ValueEntity = PojoMessageType("com.akkaserverless.scalasdk.valueentity.ValueEntity")
    val CommandContext = PojoMessageType("com.akkaserverless.scalasdk.valueentity.CommandContext")
    val ValueEntityContext = PojoMessageType("com.akkaserverless.scalasdk.valueentity.ValueEntityContext")
    val ValueEntityOptions = PojoMessageType("com.akkaserverless.scalasdk.valueentity.ValueEntityOptions")
    val ValueEntityProvider = PojoMessageType("com.akkaserverless.scalasdk.valueentity.ValueEntityProvider")
    val ValueEntityRouter = PojoMessageType("com.akkaserverless.scalasdk.impl.valueentity.ValueEntityRouter")
    val CommandHandlerNotFound = PojoMessageType(
      "com.akkaserverless.javasdk.impl.valueentity.ValueEntityRouter.CommandHandlerNotFound")
  }

  object ReplicatedEntity {

    val ReplicatedEntity = PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.Replicated")
    val CommandContext = PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.CommandContext")
    val ReplicatedEntityContext = PojoMessageType(
      "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext")
    val ReplicatedEntityOptions = PojoMessageType(
      "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityOptions")
    val ReplicatedEntityProvider = PojoMessageType(
      "com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityProvider")
    val ReplicatedEntityRouter = PojoMessageType(
      "com.akkaserverless.scalasdk.impl.replicatedentity.ReplicatedEntityRouter")
    val CommandHandlerNotFound = PojoMessageType(
      "com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityRouter.CommandHandlerNotFound")

    val ReplicatedData = PojoMessageType("com.akkaserverless.replicatedentity.ReplicatedData")

    private val dataTypes = {
      PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter") ::
      PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounterMap") ::
      PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.ReplicatedSet") ::
      PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap") ::
      PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegister") ::
      PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterMap") ::
      PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.ReplicatedMap") ::
      PojoMessageType("com.akkaserverless.scalasdk.replicatedentity.ReplicatedVote") :: Nil
    }

    def lookupReplicatedData(name: String): PojoMessageType =
      dataTypes
        .find(_.name == name)
        .getOrElse(throw new IllegalArgumentException(s"Couldn't find a MessageType definition for $name"))

  }

  val DeferredCall = PojoMessageType("com.akkaserverless.scalasdk.DeferredCall")
  val ScalaDeferredCallAdapter = PojoMessageType("com.akkaserverless.scalasdk.impl.ScalaDeferredCallAdapter")
  val InternalContext = PojoMessageType("com.akkaserverless.scalasdk.impl.InternalContext")
  val Context = PojoMessageType("com.akkaserverless.scalasdk.Context")
  val Metadata = PojoMessageType("com.akkaserverless.scalasdk.Metadata")

  val Source = PojoMessageType("akka.stream.scaladsl.Source")
  val NotUsed = PojoMessageType("akka.NotUsed")
  val ImmutableSeq = PojoMessageType("scala.collection.immutable.Seq")

  val Descriptors = PojoMessageType("com.google.protobuf.Descriptors")

  val Serializer = PojoMessageType("com.akkaserverless.javasdk.impl.Serializer")
  val Serializers = PojoMessageType("com.akkaserverless.javasdk.impl.Serializers")
  val JsonSerializer = PojoMessageType("com.akkaserverless.javasdk.impl.JsonSerializer")

  val ByteString = PojoMessageType("com.google.protobuf.ByteString")

}
