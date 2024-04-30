/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.valueentity

import java.util.Optional

import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.CollectionConverters.SetHasAsScala
import scala.jdk.OptionConverters._

import akka.stream.Materializer
import com.google.protobuf.Descriptors
import kalix.javasdk
import kalix.scalasdk.impl.InternalContext
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.valueentity.CommandContext
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import kalix.scalasdk.valueentity.ValueEntityOptions
import kalix.scalasdk.valueentity.ValueEntityProvider

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

  override def typeId(): String = scalaSdkProvider.typeId

  override def newRouter(context: javasdk.valueentity.ValueEntityContext)
      : javasdk.impl.valueentity.ValueEntityRouter[S, javasdk.valueentity.ValueEntity[S]] = {

    val scalaSdkRouter = scalaSdkProvider
      .newRouter(new ScalaValueEntityContextAdapter(context))
      .asInstanceOf[ValueEntityRouter[S, ValueEntity[S]]]

    new JavaValueEntityRouterAdapter[S](new JavaValueEntityAdapter[S](scalaSdkRouter.entity), scalaSdkRouter)
  }

  override def options(): javasdk.valueentity.ValueEntityOptions = new JavaValueEntityOptionsAdapter(
    scalaSdkProvider.options)

  override def serviceDescriptor(): Descriptors.ServiceDescriptor = scalaSdkProvider.serviceDescriptor
}

private[scalasdk] final class JavaValueEntityRouterAdapter[S](
    javaSdkValueEntity: javasdk.valueentity.ValueEntity[S],
    scalaSdkRouter: ValueEntityRouter[S, ValueEntity[S]])
    extends javasdk.impl.valueentity.ValueEntityRouter[S, javasdk.valueentity.ValueEntity[S]](javaSdkValueEntity) {

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      context: javasdk.valueentity.CommandContext): javasdk.valueentity.ValueEntity.Effect[_] = {
    scalaSdkRouter.handleCommand(commandName, state, command, new ScalaCommandContextAdapter(context)) match {
      case ValueEntityEffectImpl(javaSdkEffectImpl) => javaSdkEffectImpl
    }
  }
}

private[scalasdk] final class JavaValueEntityOptionsAdapter(scalaSdkValueEntityOptions: ValueEntityOptions)
    extends javasdk.valueentity.ValueEntityOptions {

  def forwardHeaders(): java.util.Set[String] = scalaSdkValueEntityOptions.forwardHeaders.asJava

  def withForwardHeaders(headers: java.util.Set[String]): javasdk.valueentity.ValueEntityOptions =
    new JavaValueEntityOptionsAdapter(scalaSdkValueEntityOptions.withForwardHeaders(Set.from(headers.asScala)))
}

private[scalasdk] final class ScalaCommandContextAdapter(val javaSdkContext: javasdk.valueentity.CommandContext)
    extends CommandContext
    with InternalContext {

  override def commandName: String = javaSdkContext.commandName()

  override def commandId: Long = javaSdkContext.commandId()

  override def entityId: String = javaSdkContext.entityId()

  override def metadata: kalix.scalasdk.Metadata =
    MetadataConverters.toScala(javaSdkContext.metadata())

  def getComponentGrpcClient[T](serviceClass: Class[T]): T = javaSdkContext match {
    case ctx: javasdk.impl.AbstractContext => ctx.getComponentGrpcClient(serviceClass)
  }

  override def materializer(): Materializer = javaSdkContext.materializer()
}

private[scalasdk] final class ScalaValueEntityContextAdapter(javaSdkContext: javasdk.valueentity.ValueEntityContext)
    extends ValueEntityContext {

  def entityId: String = javaSdkContext.entityId()

  override def materializer(): Materializer = javaSdkContext.materializer()
}
