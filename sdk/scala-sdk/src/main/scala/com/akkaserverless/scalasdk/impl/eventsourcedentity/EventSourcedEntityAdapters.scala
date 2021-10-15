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

package com.akkaserverless.scalasdk.impl.eventsourcedentity

import java.util.Optional
import scala.collection.immutable
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.jdk.OptionConverters._
import akka.stream.Materializer
import com.akkaserverless.javasdk
import javasdk.impl.eventsourcedentity.{ EventSourcedEntityRouter => JavaSdkEventSourcedEntityRouter }
import javasdk.eventsourcedentity.{
  CommandContext => JavaSdkCommandContext,
  EventContext => JavaSdkEventContext,
  EventSourcedEntity => JavaSdkEventSourcedEntity,
  EventSourcedEntityContext => JavaSdkEventSourcedEntityContext,
  EventSourcedEntityOptions => JavaSdkEventSourcedEntityOptions,
  EventSourcedEntityProvider => JavaSdkEventSourcedEntityProvider
}
import com.akkaserverless.scalasdk.eventsourcedentity.CommandContext
import com.akkaserverless.scalasdk.eventsourcedentity.EventContext
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityOptions
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider
import com.akkaserverless.scalasdk.ServiceCallFactory
import com.akkaserverless.scalasdk.impl.MetadataConverters
import com.akkaserverless.scalasdk.impl.PassivationStrategyConverters
import com.akkaserverless.scalasdk.impl.ScalaServiceCallFactoryAdapter
import com.google.protobuf.Descriptors

private[scalasdk] final class JavaEventSourcedEntityAdapter[S](scalasdkEventSourcedEntity: EventSourcedEntity[S])
    extends JavaSdkEventSourcedEntity[S] {

  override def emptyState(): S = scalasdkEventSourcedEntity.emptyState

  override def _internalSetEventContext(context: Optional[JavaSdkEventContext]): Unit =
    scalasdkEventSourcedEntity._internalSetEventContext(context.map(new JavaEventContextAdapter(_)).toScala)

  override def _internalSetCommandContext(context: Optional[JavaSdkCommandContext]): Unit =
    scalasdkEventSourcedEntity._internalSetCommandContext(context.map(new JavaCommandContextAdapter(_)).toScala)

}

private[scalasdk] final class JavaEventSourcedEntityProviderAdapter[S, E <: EventSourcedEntity[S]](
    scalasdkProvider: EventSourcedEntityProvider[S, E])
    extends JavaSdkEventSourcedEntityProvider[S, JavaSdkEventSourcedEntity[S]] {

  def additionalDescriptors(): Array[Descriptors.FileDescriptor] = scalasdkProvider.additionalDescriptors.toArray

  def entityType(): String = scalasdkProvider.entityType

  def newRouter(
      context: JavaSdkEventSourcedEntityContext): JavaSdkEventSourcedEntityRouter[S, JavaSdkEventSourcedEntity[S]] = {
    val scaladslRouter = scalasdkProvider
      .newRouter(new ScalaEventSourcedEntityContextAdapter(context))
      .asInstanceOf[EventSourcedEntityRouter[S, EventSourcedEntity[S]]]
    new JavaEventSourcedEntityRouterAdapter[S](
      new JavaEventSourcedEntityAdapter[S](scaladslRouter.entity),
      scaladslRouter)
  }

  def options(): JavaSdkEventSourcedEntityOptions = new JavaEventSourcedEntityOptionsAdapter(scalasdkProvider.options)
  def serviceDescriptor(): Descriptors.ServiceDescriptor = scalasdkProvider.serviceDescriptor
}

private[scalasdk] final class JavaEventSourcedEntityOptionsAdapter(
    scalasdkEventSourcedEntityOptions: EventSourcedEntityOptions)
    extends JavaSdkEventSourcedEntityOptions {

  def forwardHeaders(): java.util.Set[String] = scalasdkEventSourcedEntityOptions.forwardHeaders.asJava

  def snapshotEvery(): Int = scalasdkEventSourcedEntityOptions.snapshotEvery

  def withSnapshotEvery(numberOfEvents: Int) = new JavaEventSourcedEntityOptionsAdapter(
    scalasdkEventSourcedEntityOptions.withSnapshotEvery(numberOfEvents))

  def withForwardHeaders(headers: java.util.Set[String]): JavaSdkEventSourcedEntityOptions =
    new JavaEventSourcedEntityOptionsAdapter(
      scalasdkEventSourcedEntityOptions.withForwardHeaders(immutable.Set.from(headers.asScala)))

  def passivationStrategy(): javasdk.PassivationStrategy =
    PassivationStrategyConverters.toJava(scalasdkEventSourcedEntityOptions.passivationStrategy)

  def withPassivationStrategy(passivationStrategy: javasdk.PassivationStrategy): JavaSdkEventSourcedEntityOptions =
    new JavaEventSourcedEntityOptionsAdapter(
      scalasdkEventSourcedEntityOptions.withPassivationStrategy(
        PassivationStrategyConverters.toScala(passivationStrategy)))
}

private[scalasdk] final class JavaEventSourcedEntityRouterAdapter[S](
    javasdkEventSourcedEntity: JavaSdkEventSourcedEntity[S],
    scalasdkRouter: EventSourcedEntityRouter[S, EventSourcedEntity[S]])
    extends JavaSdkEventSourcedEntityRouter[S, JavaSdkEventSourcedEntity[S]](javasdkEventSourcedEntity) {

  override def handleEvent(state: S, event: Any): S = {
    scalasdkRouter.handleEvent(state, event)
  }

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      context: JavaSdkCommandContext): JavaSdkEventSourcedEntity.Effect[_] = {
    scalasdkRouter.handleCommand(commandName, state, command, new JavaCommandContextAdapter(context)) match {
      case EventSourcedEntityEffectImpl(javasdkEffectImpl) => javasdkEffectImpl
    }
  }
}

private[scalasdk] final class ScalaEventSourcedEntityContextAdapter(javasdkContext: JavaSdkEventSourcedEntityContext)
    extends EventSourcedEntityContext {

  def entityId: String = javasdkContext.entityId()

  override def serviceCallFactory: ServiceCallFactory =
    ScalaServiceCallFactoryAdapter(javasdkContext.serviceCallFactory())

  override def materializer(): Materializer = javasdkContext.materializer()
}

private[scalasdk] final class JavaCommandContextAdapter(val javasdkContext: JavaSdkCommandContext)
    extends CommandContext {

  override def sequenceNumber: Long = javasdkContext.sequenceNumber()

  override def commandName: String = javasdkContext.commandName()

  override def commandId: Long = javasdkContext.commandId()

  override def serviceCallFactory: ServiceCallFactory =
    ScalaServiceCallFactoryAdapter(javasdkContext.serviceCallFactory())

  override def entityId: String = javasdkContext.entityId()

  override def metadata: com.akkaserverless.scalasdk.Metadata =
    MetadataConverters.toScala(javasdkContext.metadata())

  override def materializer(): Materializer = javasdkContext.materializer()
}

private[scalasdk] final class JavaEventContextAdapter(val javasdkContext: JavaSdkEventContext) extends EventContext {
  override def sequenceNumber: Long = javasdkContext.sequenceNumber()

  override def serviceCallFactory: ServiceCallFactory =
    ScalaServiceCallFactoryAdapter(javasdkContext.serviceCallFactory())

  override def entityId: String = javasdkContext.entityId()

  override def materializer(): Materializer = javasdkContext.materializer()
}
