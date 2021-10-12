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

package com.akkaserverless.scalasdk.impl.valueentity

import java.util.Optional

import scala.collection.immutable
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.jdk.OptionConverters._

import akka.stream.Materializer
import com.akkaserverless.javasdk
import com.akkaserverless.scalasdk.ServiceCallFactory
import com.akkaserverless.scalasdk.impl.MetadataConverters
import com.akkaserverless.scalasdk.impl.MetadataImpl
import com.akkaserverless.scalasdk.impl.PassivationStrategyConverters
import com.akkaserverless.scalasdk.impl.ScalaServiceCallFactoryAdapter
import com.akkaserverless.scalasdk.valueentity.CommandContext
import com.akkaserverless.scalasdk.valueentity.ValueEntity
import com.akkaserverless.scalasdk.valueentity.ValueEntityContext
import com.akkaserverless.scalasdk.valueentity.ValueEntityOptions
import com.akkaserverless.scalasdk.valueentity.ValueEntityProvider
import com.google.protobuf.Descriptors

private[scalasdk] final class JavaValueEntityAdapter[S](scalaSdkValueEntity: ValueEntity[S])
    extends javasdk.valueentity.ValueEntity[S] {

  override def emptyState(): S = scalaSdkValueEntity.emptyState

  override def _internalSetCommandContext(context: Optional[javasdk.valueentity.CommandContext]): Unit =
    scalaSdkValueEntity._internalSetCommandContext(context.map(new ScalaCommandContextAdapter(_)).toScala)
}

private[scalasdk] final class JavaValueEntityProviderAdapter[S, E <: ValueEntity[S]](
    scalaSdkProvider: ValueEntityProvider[S, E])
    extends javasdk.valueentity.ValueEntityProvider[S, javasdk.valueentity.ValueEntity[S]] {

  override def additionalDescriptors(): Array[Descriptors.FileDescriptor] =
    scalaSdkProvider.additionalDescriptors.toArray

  override def entityType(): String = scalaSdkProvider.entityType

  override def newRouter(context: javasdk.valueentity.ValueEntityContext)
      : javasdk.impl.valueentity.ValueEntityRouter[S, javasdk.valueentity.ValueEntity[S]] = {

    val scalaSdkHandler = scalaSdkProvider
      .newRouter(new ScalaValueEntityContextAdapter(context))
      .asInstanceOf[ValueEntityRouter[S, ValueEntity[S]]]

    new JavaValueEntityRouterAdapter[S](new JavaValueEntityAdapter[S](scalaSdkHandler.entity), scalaSdkHandler)
  }

  override def options(): javasdk.valueentity.ValueEntityOptions = new JavaValueEntityOptionsAdapter(
    scalaSdkProvider.options)

  override def serviceDescriptor(): Descriptors.ServiceDescriptor = scalaSdkProvider.serviceDescriptor
}

private[scalasdk] final class JavaValueEntityRouterAdapter[S](
    javaSdkValueEntity: javasdk.valueentity.ValueEntity[S],
    scalaSdkHandler: ValueEntityRouter[S, ValueEntity[S]])
    extends javasdk.impl.valueentity.ValueEntityRouter[S, javasdk.valueentity.ValueEntity[S]](javaSdkValueEntity) {

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      context: javasdk.valueentity.CommandContext): javasdk.valueentity.ValueEntity.Effect[_] = {
    scalaSdkHandler.handleCommand(commandName, state, command, new ScalaCommandContextAdapter(context)) match {
      case ValueEntityEffectImpl(javaSdkEffectImpl) => javaSdkEffectImpl
    }
  }
}

private[scalasdk] final class JavaValueEntityOptionsAdapter(scalaSdkValueEntityOptions: ValueEntityOptions)
    extends javasdk.valueentity.ValueEntityOptions {

  def forwardHeaders(): java.util.Set[String] = scalaSdkValueEntityOptions.forwardHeaders.asJava

  def withForwardHeaders(headers: java.util.Set[String]): javasdk.valueentity.ValueEntityOptions =
    new JavaValueEntityOptionsAdapter(
      scalaSdkValueEntityOptions.withForwardHeaders(immutable.Set.from(headers.asScala)))

  def passivationStrategy(): javasdk.PassivationStrategy =
    PassivationStrategyConverters.toJava(scalaSdkValueEntityOptions.passivationStrategy)

  def withPassivationStrategy(
      passivationStrategy: javasdk.PassivationStrategy): javasdk.valueentity.ValueEntityOptions =
    new JavaValueEntityOptionsAdapter(
      scalaSdkValueEntityOptions.withPassivationStrategy(PassivationStrategyConverters.toScala(passivationStrategy)))
}

private[scalasdk] final class ScalaCommandContextAdapter(val javaSdkContext: javasdk.valueentity.CommandContext)
    extends CommandContext {

  override def commandName: String = javaSdkContext.commandName()

  override def commandId: Long = javaSdkContext.commandId()

  override def getGrpcClient[T](clientClass: Class[T], service: String): T =
    javaSdkContext.getGrpcClient(clientClass, service)

  override def serviceCallFactory: ServiceCallFactory =
    ScalaServiceCallFactoryAdapter(javaSdkContext.serviceCallFactory())

  override def entityId: String = javaSdkContext.entityId()

  override def metadata: com.akkaserverless.scalasdk.Metadata =
    MetadataConverters.toScala(javaSdkContext.metadata())

  override def materializer(): Materializer = javaSdkContext.materializer()
}

private[scalasdk] final class ScalaValueEntityContextAdapter(javaSdkContext: javasdk.valueentity.ValueEntityContext)
    extends ValueEntityContext {

  def entityId: String = javaSdkContext.entityId()

  override def getGrpcClient[T](clientClass: Class[T], service: String): T =
    javaSdkContext.getGrpcClient(clientClass, service)

  override def serviceCallFactory: ServiceCallFactory =
    ScalaServiceCallFactoryAdapter(javaSdkContext.serviceCallFactory())

  override def materializer(): Materializer = javaSdkContext.materializer()
}
