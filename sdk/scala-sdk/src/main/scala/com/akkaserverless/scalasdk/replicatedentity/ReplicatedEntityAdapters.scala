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

package com.akkaserverless.scalasdk.replicatedentity

import akka.stream.Materializer
import com.akkaserverless.javasdk.impl.replicatedentity.{ ReplicatedEntityHandler => JavaSdkReplicatedEntityHandler }
import com.akkaserverless.javasdk.impl.{ ComponentOptions => JavaSdkComponentOptions }
import com.akkaserverless.javasdk.replicatedentity.{
  CommandContext => JavaSdkCommandContext,
  ReplicatedCounter => JavaSdkReplicatedCounter,
  ReplicatedCounterMap => JavaSdkReplicatedCounterMap,
  ReplicatedDataFactory => JavaSdkReplicatedDataFactory,
  ReplicatedEntity => JavaSdkReplicatedEntity,
  ReplicatedEntityContext => JavaSdkReplicatedEntityContext,
  ReplicatedEntityOptions => JavaSdkReplicatedEntityOptions,
  ReplicatedEntityProvider => JavaSdkReplicatedEntityProvider,
  ReplicatedMap => JavaSdkReplicatedMap,
  ReplicatedMultiMap => JavaSdkReplicatedMultiMap,
  ReplicatedRegister => JavaSdkReplicatedRegister,
  ReplicatedRegisterMap => JavaSdkReplicatedRegisterMap,
  ReplicatedSet => JavaSdkReplicatedSet,
  ReplicatedVote => JavaSdkReplicatedVote,
  WriteConsistency => JavaSdkWriteConsistency
}
import com.akkaserverless.javasdk.{ PassivationStrategy => JavaSdkPassivationStrategy }
import com.akkaserverless.replicatedentity.ReplicatedData
import com.akkaserverless.scalasdk.{ Metadata, ServiceCallFactory }
import com.akkaserverless.scalasdk.impl.{
  MetadataConverters,
  PassivationStrategyConverters,
  ScalaServiceCallFactoryAdapter
}
import com.akkaserverless.scalasdk.impl.replicatedentity.{ ReplicatedEntityEffectImpl, ReplicatedEntityHandler }
import com.google.protobuf.Descriptors

import java.util
import java.util.Optional
import scala.collection.immutable
import scala.jdk.CollectionConverters.{ SetHasAsJava, SetHasAsScala }
import scala.jdk.OptionConverters.RichOptional

private[scalasdk] final case class JavaReplicatedEntityProviderAdapter[D <: ReplicatedData, E <: ReplicatedEntity[D]](
    scalaSdkProvider: ReplicatedEntityProvider[D, E])
    extends JavaSdkReplicatedEntityProvider[D, JavaSdkReplicatedEntity[D]] {

  override def options(): JavaSdkReplicatedEntityOptions =
    JavaReplicatedEntityOptionsAdapter(scalaSdkProvider.options)

  override def serviceDescriptor(): Descriptors.ServiceDescriptor =
    scalaSdkProvider.serviceDescriptor

  override def entityType(): String = scalaSdkProvider.entityType

  override def newHandler(
      context: JavaSdkReplicatedEntityContext): JavaSdkReplicatedEntityHandler[D, JavaSdkReplicatedEntity[D]] = {

    val scalaSdkHandler = scalaSdkProvider.newHandler(ScalaReplicatedEntityContextAdapter(context))

    JavaReplicatedEntityHandlerAdapter(JavaReplicatedEntityAdapter(scalaSdkHandler.entity), scalaSdkHandler)
  }

  override def additionalDescriptors(): Array[Descriptors.FileDescriptor] =
    scalaSdkProvider.additionalDescriptors.toArray
}

private[scalasdk] final case class JavaReplicatedEntityHandlerAdapter[D <: ReplicatedData, E <: ReplicatedEntity[D]](
    javaSdkReplicatedEntity: JavaSdkReplicatedEntity[D],
    scalaSdkHandler: ReplicatedEntityHandler[D, E])
    extends JavaSdkReplicatedEntityHandler[D, JavaSdkReplicatedEntity[D]](javaSdkReplicatedEntity) {

  override protected def handleCommand(
      commandName: String,
      data: D,
      command: Any,
      context: JavaSdkCommandContext): JavaSdkReplicatedEntity.Effect[_] = {

    val scalaData = ReplicatedDataConverter.toScala(data)
    scalaSdkHandler.handleCommand(commandName, scalaData, command, ScalaCommandContextAdapter(context)) match {
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
    JavaReplicatedEntityOptionsAdapter(scalaSdkOptions.withForwardHeaders(immutable.Set.from(headers.asScala)))
}

private[scalasdk] final case class ScalaCommandContextAdapter(javaSdkCommandContext: JavaSdkCommandContext)
    extends CommandContext {

  override def metadata: Metadata =
    MetadataConverters.toScala(javaSdkCommandContext.metadata())

  override def entityId: String = javaSdkCommandContext.entityId()

  override def serviceCallFactory: ServiceCallFactory =
    ScalaServiceCallFactoryAdapter(javaSdkCommandContext.serviceCallFactory())

  override def getGrpcClient[T](clientClass: Class[T], service: String): T =
    javaSdkCommandContext.getGrpcClient(clientClass, service)

  override def materializer(): Materializer =
    javaSdkCommandContext.materializer()

}

private[scalasdk] final case class ScalaReplicatedEntityContextAdapter(javaSdkContext: JavaSdkReplicatedEntityContext)
    extends ReplicatedEntityContext {

  override def entityId: String = javaSdkContext.entityId()

  override def serviceCallFactory: ServiceCallFactory = ScalaServiceCallFactoryAdapter(
    javaSdkContext.serviceCallFactory())

  override def getGrpcClient[T](clientClass: Class[T], service: String): T =
    javaSdkContext.getGrpcClient(clientClass, service)

  override def materializer(): Materializer =
    javaSdkContext.materializer()
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
    new ReplicatedCounter(factory.newCounter())

  /** Create a new map of counters. */
  override def newReplicatedCounterMap[K]: ReplicatedCounterMap[K] =
    new ReplicatedCounterMap[K](factory.newReplicatedCounterMap())

  /** Create a new ReplicatedSet. */
  override def newReplicatedSet[E]: ReplicatedSet[E] =
    new ReplicatedSet[E](factory.newReplicatedSet())

  /** Create a new multimap (map of sets). */
  override def newReplicatedMultiMap[K, V]: ReplicatedMultiMap[K, V] =
    new ReplicatedMultiMap(factory.newReplicatedMultiMap())

  /** Create a new ReplicatedRegister. */
  override def newRegister[T](value: T): ReplicatedRegister[T] =
    new ReplicatedRegister(factory.newRegister(value))

  /** Create a new map of registers. */
  override def newReplicatedRegisterMap[K, V]: ReplicatedRegisterMap[K, V] =
    new ReplicatedRegisterMap(factory.newReplicatedRegisterMap())

  /** Create a new ReplicatedMap. */
  override def newReplicatedMap[K, V <: ReplicatedData]: ReplicatedMap[K, V] =
    new ReplicatedMap(factory.newReplicatedMap())

  /** Create a new Vote. */
  override def newVote: ReplicatedVote =
    new ReplicatedVote(factory.newVote())
}

private[scalasdk] object ReplicatedDataConverter {
  def toScala[D <: ReplicatedData](data: D): ReplicatedData = {
    data match {
      case d: JavaSdkReplicatedCounter           => new ReplicatedCounter(d)
      case d: JavaSdkReplicatedCounterMap[_]     => new ReplicatedCounterMap(d)
      case d: JavaSdkReplicatedSet[_]            => new ReplicatedSet(d)
      case d: JavaSdkReplicatedMultiMap[_, _]    => new ReplicatedMultiMap(d)
      case d: JavaSdkReplicatedRegister[_]       => new ReplicatedRegister(d)
      case d: JavaSdkReplicatedRegisterMap[_, _] => new ReplicatedRegisterMap(d)
      case d: JavaSdkReplicatedMap[_, _]         => new ReplicatedMap(d)
      case d: JavaSdkReplicatedVote              => new ReplicatedVote(d)
    }
  }
}
