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

import akka.stream.Materializer
import com.akkaserverless.javasdk
import com.akkaserverless.javasdk.eventsourcedentity.{ EventSourcedEntityProvider => JavaSdkEventSourcedEntityProvider }
import com.akkaserverless.javasdk.eventsourcedentity.{ EventSourcedEntityContext => JavaSdkEventSourcedEntityContext }
import com.akkaserverless.javasdk.eventsourcedentity.{ EventSourcedEntity => JavaSdkEventSourcedEntity }
import com.akkaserverless.javasdk.eventsourcedentity.{ EventContext => JavaSdkEventContext }
import com.akkaserverless.javasdk.eventsourcedentity.{ EventSourcedEntityOptions => JavaSdkEventSourcedEntityOptions }
import com.akkaserverless.javasdk.eventsourcedentity.{ CommandContext => JavaSdkCommandContext }
import com.akkaserverless.javasdk.impl.eventsourcedentity.{
  EventSourcedEntityRouter => JavaSdkEventSourcedEntityRouter
}
import com.akkaserverless.scalasdk.eventsourcedentity.CommandContext
import com.akkaserverless.scalasdk.eventsourcedentity.EventContext
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityOptions
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider
import com.akkaserverless.scalasdk.impl.MetadataConverters
import com.akkaserverless.scalasdk.impl.PassivationStrategyConverters
import com.google.protobuf.Descriptors

import java.util.Optional
import scala.collection.immutable.Set
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.jdk.OptionConverters._

private[scalasdk] final class JavaEventSourcedEntityAdapter[S](scalaSdkEventSourcedEntity: EventSourcedEntity[S])
    extends JavaSdkEventSourcedEntity[S] {

  override def emptyState(): S = scalaSdkEventSourcedEntity.emptyState

  override def _internalSetEventContext(context: Optional[JavaSdkEventContext]): Unit =
    scalaSdkEventSourcedEntity._internalSetEventContext(context.map(new JavaEventContextAdapter(_)).toScala)

  override def _internalSetCommandContext(context: Optional[JavaSdkCommandContext]): Unit =
    scalaSdkEventSourcedEntity._internalSetCommandContext(context.map(new JavaCommandContextAdapter(_)).toScala)

}

private[scalasdk] final class JavaEventSourcedEntityProviderAdapter[S, E <: EventSourcedEntity[S]](
    scalaSdkProvider: EventSourcedEntityProvider[S, E])
    extends JavaSdkEventSourcedEntityProvider[S, JavaSdkEventSourcedEntity[S]] {

  def additionalDescriptors(): Array[Descriptors.FileDescriptor] = scalaSdkProvider.additionalDescriptors.toArray

  def entityType(): String = scalaSdkProvider.entityType

  def newRouter(
      context: JavaSdkEventSourcedEntityContext): JavaSdkEventSourcedEntityRouter[S, JavaSdkEventSourcedEntity[S]] = {
    val scaladslRouter = scalaSdkProvider
      .newRouter(new ScalaEventSourcedEntityContextAdapter(context))
      .asInstanceOf[EventSourcedEntityRouter[S, EventSourcedEntity[S]]]
    new JavaEventSourcedEntityRouterAdapter[S](
      new JavaEventSourcedEntityAdapter[S](scaladslRouter.entity),
      scaladslRouter)
  }

  def options(): JavaSdkEventSourcedEntityOptions = new JavaEventSourcedEntityOptionsAdapter(scalaSdkProvider.options)
  def serviceDescriptor(): Descriptors.ServiceDescriptor = scalaSdkProvider.serviceDescriptor
}

private[scalasdk] final class JavaEventSourcedEntityOptionsAdapter(
    scalaSdkEventSourcedEntityOptions: EventSourcedEntityOptions)
    extends JavaSdkEventSourcedEntityOptions {

  def forwardHeaders(): java.util.Set[String] = scalaSdkEventSourcedEntityOptions.forwardHeaders.asJava

  def snapshotEvery(): Int = scalaSdkEventSourcedEntityOptions.snapshotEvery

  def withSnapshotEvery(numberOfEvents: Int) = new JavaEventSourcedEntityOptionsAdapter(
    scalaSdkEventSourcedEntityOptions.withSnapshotEvery(numberOfEvents))

  def withForwardHeaders(headers: java.util.Set[String]): JavaSdkEventSourcedEntityOptions =
    new JavaEventSourcedEntityOptionsAdapter(
      scalaSdkEventSourcedEntityOptions.withForwardHeaders(Set.from(headers.asScala)))

  def passivationStrategy(): javasdk.PassivationStrategy =
    PassivationStrategyConverters.toJava(scalaSdkEventSourcedEntityOptions.passivationStrategy)

  def withPassivationStrategy(passivationStrategy: javasdk.PassivationStrategy): JavaSdkEventSourcedEntityOptions =
    new JavaEventSourcedEntityOptionsAdapter(
      scalaSdkEventSourcedEntityOptions.withPassivationStrategy(
        PassivationStrategyConverters.toScala(passivationStrategy)))
}

private[scalasdk] final class JavaEventSourcedEntityRouterAdapter[S](
    javaSdkEventSourcedEntity: JavaSdkEventSourcedEntity[S],
    scalaSdkRouter: EventSourcedEntityRouter[S, EventSourcedEntity[S]])
    extends JavaSdkEventSourcedEntityRouter[S, JavaSdkEventSourcedEntity[S]](javaSdkEventSourcedEntity) {

  override def handleEvent(state: S, event: Any): S = {
    scalaSdkRouter.handleEvent(state, event)
  }

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      context: JavaSdkCommandContext): JavaSdkEventSourcedEntity.Effect[_] = {
    scalaSdkRouter.handleCommand(commandName, state, command, new JavaCommandContextAdapter(context)) match {
      case EventSourcedEntityEffectImpl(javasdkEffectImpl) => javasdkEffectImpl
    }
  }
}

private[scalasdk] final class ScalaEventSourcedEntityContextAdapter(javaSdkContext: JavaSdkEventSourcedEntityContext)
    extends EventSourcedEntityContext {

  def entityId: String = javaSdkContext.entityId()

  override def materializer(): Materializer = javaSdkContext.materializer()
}

private[scalasdk] final class JavaCommandContextAdapter(val javaSdkContext: JavaSdkCommandContext)
    extends CommandContext {

  override def sequenceNumber: Long = javaSdkContext.sequenceNumber()

  override def commandName: String = javaSdkContext.commandName()

  override def commandId: Long = javaSdkContext.commandId()

  override def entityId: String = javaSdkContext.entityId()

  override def metadata: com.akkaserverless.scalasdk.Metadata =
    MetadataConverters.toScala(javaSdkContext.metadata())

  override def materializer(): Materializer = javaSdkContext.materializer()
}

private[scalasdk] final class JavaEventContextAdapter(val javasdkContext: JavaSdkEventContext) extends EventContext {
  override def sequenceNumber: Long = javasdkContext.sequenceNumber()

  override def entityId: String = javasdkContext.entityId()

  override def materializer(): Materializer = javasdkContext.materializer()
}
