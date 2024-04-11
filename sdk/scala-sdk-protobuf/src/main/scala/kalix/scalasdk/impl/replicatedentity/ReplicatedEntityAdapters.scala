/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.replicatedentity

import akka.stream.Materializer
import kalix.javasdk.impl.AbstractContext
import kalix.javasdk.impl.replicatedentity.ReplicatedCounterImpl
import kalix.javasdk.impl.replicatedentity.ReplicatedCounterMapImpl
import kalix.javasdk.impl.replicatedentity.ReplicatedMapImpl
import kalix.javasdk.impl.replicatedentity.ReplicatedMultiMapImpl
import kalix.javasdk.impl.replicatedentity.ReplicatedRegisterImpl
import kalix.javasdk.impl.replicatedentity.ReplicatedRegisterMapImpl
import kalix.javasdk.impl.replicatedentity.ReplicatedSetImpl
import kalix.javasdk.impl.replicatedentity.ReplicatedVoteImpl
import kalix.javasdk.impl.replicatedentity.{ ReplicatedEntityRouter => JavaSdkReplicatedEntityRouter }
import kalix.javasdk.impl.{ ComponentOptions => JavaSdkComponentOptions }
import kalix.javasdk.replicatedentity.{ CommandContext => JavaSdkCommandContext }
import kalix.javasdk.replicatedentity.{ ReplicatedDataFactory => JavaSdkReplicatedDataFactory }
import kalix.javasdk.replicatedentity.{ ReplicatedEntity => JavaSdkReplicatedEntity }
import kalix.javasdk.replicatedentity.{ ReplicatedEntityContext => JavaSdkReplicatedEntityContext }
import kalix.javasdk.replicatedentity.{ ReplicatedEntityOptions => JavaSdkReplicatedEntityOptions }
import kalix.javasdk.replicatedentity.{ ReplicatedEntityProvider => JavaSdkReplicatedEntityProvider }
import kalix.javasdk.replicatedentity.{ WriteConsistency => JavaSdkWriteConsistency }
import kalix.javasdk.{ PassivationStrategy => JavaSdkPassivationStrategy }
import kalix.replicatedentity.ReplicatedData
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.InternalContext
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.impl.PassivationStrategyConverters
import kalix.scalasdk.replicatedentity.CommandContext
import kalix.scalasdk.replicatedentity.ReplicatedCounter
import kalix.scalasdk.replicatedentity.ReplicatedCounterMap
import kalix.scalasdk.replicatedentity.ReplicatedDataFactory
import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedEntityContext
import kalix.scalasdk.replicatedentity.ReplicatedEntityOptions
import kalix.scalasdk.replicatedentity.ReplicatedEntityProvider
import kalix.scalasdk.replicatedentity.ReplicatedMap
import kalix.scalasdk.replicatedentity.ReplicatedMultiMap
import kalix.scalasdk.replicatedentity.ReplicatedRegister
import kalix.scalasdk.replicatedentity.ReplicatedRegisterMap
import kalix.scalasdk.replicatedentity.ReplicatedSet
import kalix.scalasdk.replicatedentity.ReplicatedVote
import kalix.scalasdk.replicatedentity.WriteConsistency
import com.google.protobuf.Descriptors
import java.util
import java.util.Optional

import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.jdk.OptionConverters.RichOptional

private[scalasdk] final case class JavaReplicatedEntityProviderAdapter[D <: ReplicatedData, E <: ReplicatedEntity[D]](
    scalaSdkProvider: ReplicatedEntityProvider[D, E])
    extends JavaSdkReplicatedEntityProvider[D, JavaSdkReplicatedEntity[D]] {

  override def options(): JavaSdkReplicatedEntityOptions =
    JavaReplicatedEntityOptionsAdapter(scalaSdkProvider.options)

  override def serviceDescriptor(): Descriptors.ServiceDescriptor =
    scalaSdkProvider.serviceDescriptor

  override def typeId(): String = scalaSdkProvider.typeId

  override def newRouter(
      context: JavaSdkReplicatedEntityContext): JavaSdkReplicatedEntityRouter[D, JavaSdkReplicatedEntity[D]] = {

    val scalaSdkRouter = scalaSdkProvider.newRouter(ScalaReplicatedEntityContextAdapter(context))

    JavaReplicatedEntityRouterAdapter(JavaReplicatedEntityAdapter(scalaSdkRouter.entity), scalaSdkRouter)
  }

  override def additionalDescriptors(): Array[Descriptors.FileDescriptor] =
    scalaSdkProvider.additionalDescriptors.toArray
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

    scalaSdkRouter.handleCommand(
      commandName,
      ScalaReplicatedDataConverter.convert(data),
      command,
      ScalaCommandContextAdapter(context)) match {
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

private[scalasdk] object ScalaReplicatedDataConverter {

  def convert[D <: ReplicatedData](data: D): D =
    data match {
      case counter: ReplicatedCounterImpl =>
        new ReplicatedCounter(counter).asInstanceOf[D]
      case register: ReplicatedRegisterImpl[D @unchecked] =>
        new ReplicatedRegister[D](register).asInstanceOf[D]
      case set: ReplicatedSetImpl[D @unchecked] =>
        new ReplicatedSet[D](set).asInstanceOf[D]
      case counterMap: ReplicatedCounterMapImpl[D @unchecked] =>
        new ReplicatedCounterMap[D](counterMap).asInstanceOf[D]
      case registerMap: ReplicatedRegisterMapImpl[Any @unchecked, Any @unchecked] =>
        new ReplicatedRegisterMap[Any, Any](registerMap).asInstanceOf[D]
      case multiMap: ReplicatedMultiMapImpl[Any @unchecked, Any @unchecked] =>
        new ReplicatedMultiMap[Any, Any](multiMap).asInstanceOf[D]
      case map: ReplicatedMapImpl[Any @unchecked, D @unchecked] =>
        new ReplicatedMap[Any, D](map).asInstanceOf[D]
      case _ => data
    }
}
