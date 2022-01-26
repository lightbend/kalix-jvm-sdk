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

package com.akkaserverless.scalasdk.impl.replicatedentity

import akka.stream.Materializer
import com.akkaserverless.javasdk.impl.AbstractContext
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedCounterImpl
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedCounterMapImpl
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedMapImpl
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedMultiMapImpl
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedRegisterImpl
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedRegisterMapImpl
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedSetImpl
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedVoteImpl
import com.akkaserverless.javasdk.impl.replicatedentity.{ ReplicatedEntityRouter => JavaSdkReplicatedEntityRouter }
import com.akkaserverless.javasdk.impl.{ ComponentOptions => JavaSdkComponentOptions }
import com.akkaserverless.javasdk.replicatedentity.{ CommandContext => JavaSdkCommandContext }
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedDataFactory => JavaSdkReplicatedDataFactory }
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedEntity => JavaSdkReplicatedEntity }
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedEntityContext => JavaSdkReplicatedEntityContext }
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedEntityOptions => JavaSdkReplicatedEntityOptions }
import com.akkaserverless.javasdk.replicatedentity.{ ReplicatedEntityProvider => JavaSdkReplicatedEntityProvider }
import com.akkaserverless.javasdk.replicatedentity.{ WriteConsistency => JavaSdkWriteConsistency }
import com.akkaserverless.javasdk.{ PassivationStrategy => JavaSdkPassivationStrategy }
import com.akkaserverless.replicatedentity.ReplicatedData
import com.akkaserverless.scalasdk.Metadata
import com.akkaserverless.scalasdk.impl.InternalContext
import com.akkaserverless.scalasdk.impl.MetadataConverters
import com.akkaserverless.scalasdk.impl.PassivationStrategyConverters
import com.akkaserverless.scalasdk.replicatedentity.CommandContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounterMap
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedDataFactory
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityOptions
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityProvider
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMap
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedMultiMap
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegister
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedRegisterMap
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedSet
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedVote
import com.akkaserverless.scalasdk.replicatedentity.WriteConsistency
import com.google.protobuf.Descriptors
import java.util
import java.util.Optional

import scala.collection.immutable.Set
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.jdk.OptionConverters.RichOptional

import com.akkaserverless.javasdk.impl.Serializer

private[scalasdk] final case class JavaReplicatedEntityProviderAdapter[D <: ReplicatedData, E <: ReplicatedEntity[D]](
    scalaSdkProvider: ReplicatedEntityProvider[D, E])
    extends JavaSdkReplicatedEntityProvider[D, JavaSdkReplicatedEntity[D]] {

  override def options(): JavaSdkReplicatedEntityOptions =
    JavaReplicatedEntityOptionsAdapter(scalaSdkProvider.options)

  override def serviceDescriptor(): Descriptors.ServiceDescriptor =
    scalaSdkProvider.serviceDescriptor

  override def entityType(): String = scalaSdkProvider.entityType

  override def newRouter(
      context: JavaSdkReplicatedEntityContext): JavaSdkReplicatedEntityRouter[D, JavaSdkReplicatedEntity[D]] = {

    val scalaSdkRouter = scalaSdkProvider.newRouter(ScalaReplicatedEntityContextAdapter(context))

    JavaReplicatedEntityRouterAdapter(JavaReplicatedEntityAdapter(scalaSdkRouter.entity), scalaSdkRouter)
  }

  override def additionalDescriptors(): Array[Descriptors.FileDescriptor] =
    scalaSdkProvider.additionalDescriptors.toArray

  override def serializer(): Serializer = scalaSdkProvider.serializer
}

private[scalasdk] final case class JavaReplicatedEntityRouterAdapter[D <: ReplicatedData, E <: ReplicatedEntity[D]](
    javaSdkReplicatedEntity: JavaSdkReplicatedEntity[D],
    scalaSdkRouter: ReplicatedEntityRouter[D, E])
    extends JavaSdkReplicatedEntityRouter[D, JavaSdkReplicatedEntity[D]](javaSdkReplicatedEntity) {

  override protected def handleCommand(
      commandName: String,
      data: D,
      command: Any,
      context: JavaSdkCommandContext): JavaSdkReplicatedEntity.Effect[_] = {

    scalaSdkRouter.handleCommand(commandName, data, command, ScalaCommandContextAdapter(context)) match {
      case ReplicatedEntityEffectImpl(javaSdkEffect) => javaSdkEffect
    }
  }
}

private[scalasdk] final case class JavaReplicatedEntityOptionsAdapter(scalaSdkOptions: ReplicatedEntityOptions)
    extends JavaSdkReplicatedEntityOptions {

  override def withPassivationStrategy(javaSdkStrategy: JavaSdkPassivationStrategy): JavaSdkReplicatedEntityOptions = {
    val scalaSdkStrategy = PassivationStrategyConverters.toScala(javaSdkStrategy)
    JavaReplicatedEntityOptionsAdapter(scalaSdkOptions.withPassivationStrategy(scalaSdkStrategy))
  }

  override def writeConsistency(): JavaSdkWriteConsistency =
    scalaSdkOptions.writeConsistency match {
      case WriteConsistency.Local    => JavaSdkWriteConsistency.LOCAL
      case WriteConsistency.Majority => JavaSdkWriteConsistency.MAJORITY
      case WriteConsistency.All      => JavaSdkWriteConsistency.ALL
    }

  override def withWriteConsistency(writeConsistency: JavaSdkWriteConsistency): JavaSdkReplicatedEntityOptions = {
    val scalaWriteConsistency =
      writeConsistency match {
        case JavaSdkWriteConsistency.LOCAL    => WriteConsistency.Local
        case JavaSdkWriteConsistency.MAJORITY => WriteConsistency.Majority
        case JavaSdkWriteConsistency.ALL      => WriteConsistency.All
      }
    JavaReplicatedEntityOptionsAdapter(scalaSdkOptions.withWriteConsistency(scalaWriteConsistency))
  }

  override def passivationStrategy(): JavaSdkPassivationStrategy =
    PassivationStrategyConverters.toJava(scalaSdkOptions.passivationStrategy)

  override def forwardHeaders(): util.Set[String] =
    scalaSdkOptions.forwardHeaders.asJava

  override def withForwardHeaders(headers: util.Set[String]): JavaSdkComponentOptions =
    JavaReplicatedEntityOptionsAdapter(scalaSdkOptions.withForwardHeaders(Set.from(headers.asScala)))
}

private[scalasdk] final case class ScalaCommandContextAdapter(javaSdkCommandContext: JavaSdkCommandContext)
    extends CommandContext {

  override def metadata: Metadata =
    MetadataConverters.toScala(javaSdkCommandContext.metadata())

  override def entityId: String = javaSdkCommandContext.entityId()

  override def materializer(): Materializer =
    javaSdkCommandContext.materializer()

}

private[scalasdk] final case class ScalaReplicatedEntityContextAdapter(javaSdkContext: JavaSdkReplicatedEntityContext)
    extends ReplicatedEntityContext
    with InternalContext {

  override def entityId: String = javaSdkContext.entityId()

  override def materializer(): Materializer =
    javaSdkContext.materializer()

  def getComponentGrpcClient[T](serviceClass: Class[T]): T = javaSdkContext match {
    case ctx: AbstractContext => ctx.getComponentGrpcClient(serviceClass)
  }

}

private[scalasdk] final case class JavaReplicatedEntityAdapter[D <: ReplicatedData](
    scalaSdkReplicatedEntity: ReplicatedEntity[D])
    extends JavaSdkReplicatedEntity[D] {

  override def emptyData(factory: JavaSdkReplicatedDataFactory): D =
    scalaSdkReplicatedEntity.emptyData(ScalaReplicatedDataFactoryAdapter(factory))

  /** INTERNAL API */
  override def _internalSetCommandContext(context: Optional[JavaSdkCommandContext]): Unit =
    scalaSdkReplicatedEntity._internalSetCommandContext(context.map(ScalaCommandContextAdapter(_)).toScala)
}

private[scalasdk] final case class ScalaReplicatedDataFactoryAdapter(factory: JavaSdkReplicatedDataFactory)
    extends ReplicatedDataFactory {

  /** Create a new counter. */
  override def newCounter: ReplicatedCounter =
    new ReplicatedCounter(factory.newCounter().asInstanceOf[ReplicatedCounterImpl])

  /** Create a new map of counters. */
  override def newReplicatedCounterMap[K]: ReplicatedCounterMap[K] =
    new ReplicatedCounterMap[K](factory.newReplicatedCounterMap().asInstanceOf[ReplicatedCounterMapImpl[K]])

  /** Create a new ReplicatedSet. */
  override def newReplicatedSet[E]: ReplicatedSet[E] =
    new ReplicatedSet[E](factory.newReplicatedSet().asInstanceOf[ReplicatedSetImpl[E]])

  /** Create a new multimap (map of sets). */
  override def newReplicatedMultiMap[K, V]: ReplicatedMultiMap[K, V] =
    new ReplicatedMultiMap(factory.newReplicatedMultiMap().asInstanceOf[ReplicatedMultiMapImpl[K, V]])

  /** Create a new ReplicatedRegister. */
  override def newRegister[T](value: T): ReplicatedRegister[T] =
    new ReplicatedRegister(factory.newRegister(value).asInstanceOf[ReplicatedRegisterImpl[T]])

  /** Create a new map of registers. */
  override def newReplicatedRegisterMap[K, V]: ReplicatedRegisterMap[K, V] =
    new ReplicatedRegisterMap(factory.newReplicatedRegisterMap().asInstanceOf[ReplicatedRegisterMapImpl[K, V]])

  /** Create a new ReplicatedMap. */
  override def newReplicatedMap[K, V <: ReplicatedData]: ReplicatedMap[K, V] =
    new ReplicatedMap(factory.newReplicatedMap().asInstanceOf[ReplicatedMapImpl[K, V]])

  /** Create a new Vote. */
  override def newVote: ReplicatedVote =
    new ReplicatedVote(factory.newVote().asInstanceOf[ReplicatedVoteImpl])
}
