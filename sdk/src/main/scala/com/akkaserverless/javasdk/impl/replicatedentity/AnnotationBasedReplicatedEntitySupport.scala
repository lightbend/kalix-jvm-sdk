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

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk.replicatedentity._
import com.akkaserverless.javasdk.impl.ReflectionHelper._
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.javasdk.{EntityFactory, Metadata, Reply, ServiceCall, ServiceCallFactory}
import com.google.protobuf.{Descriptors, Any => JavaPbAny}
import java.lang.reflect.{Constructor, Executable, InvocationTargetException}
import java.util.function.Consumer
import java.util.{function, Optional}
import scala.reflect.ClassTag

/**
 * Annotation based implementation of the [[ReplicatedEntityHandlerFactory]].
 */
private[impl] class AnnotationBasedReplicatedEntitySupport(
    entityClass: Class[_],
    anySupport: AnySupport,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]],
    factory: Option[ReplicatedEntityCreationContext => AnyRef] = None
) extends ReplicatedEntityHandlerFactory
    with ResolvedEntityFactory {

  def this(entityClass: Class[_], anySupport: AnySupport, serviceDescriptor: Descriptors.ServiceDescriptor) =
    this(entityClass, anySupport, anySupport.resolveServiceDescriptor(serviceDescriptor))

  def this(factory: EntityFactory, anySupport: AnySupport, serviceDescriptor: Descriptors.ServiceDescriptor) =
    this(factory.entityClass,
         anySupport,
         anySupport.resolveServiceDescriptor(serviceDescriptor),
         Some(context => factory.create(context)))

  private val constructor: ReplicatedEntityCreationContext => AnyRef = factory.getOrElse {
    entityClass.getConstructors match {
      case Array(single) =>
        new EntityConstructorInvoker(ReflectionHelper.ensureAccessible(single))
      case _ =>
        throw new RuntimeException(s"Only a single constructor is allowed on Replicated Entity: $entityClass")
    }
  }

  private val (commandHandlers, streamedCommandHandlers) = {
    val allMethods = ReflectionHelper.getAllDeclaredMethods(entityClass)

    ReflectionHelper.validateNoBadMethods(allMethods, classOf[ReplicatedEntity], Set(classOf[CommandHandler]))
    val handlers = allMethods
      .filter(_.getAnnotation(classOf[CommandHandler]) != null)
      .map { method =>
        val annotation = method.getAnnotation(classOf[CommandHandler])
        val name: String = if (annotation.name().isEmpty) {
          ReflectionHelper.getCapitalizedName(method)
        } else annotation.name()

        val serviceMethod = resolvedMethods.getOrElse(name, {
          throw new RuntimeException(
            s"Command handler method ${method.getName} for command $name found, but the service has no command by that name."
          )
        })
        (ReflectionHelper.ensureAccessible(method), serviceMethod)
      }

    def getHandlers[C <: ReplicatedEntityContext with ReplicatedDataFactory: ClassTag](streamed: Boolean) =
      handlers
        .filter(_._2.outputStreamed == streamed)
        .map {
          case (method, serviceMethod) =>
            new CommandHandlerInvoker[C](method,
                                         serviceMethod,
                                         anySupport,
                                         ReplicatedEntityAnnotationHelper.replicatedEntityParameterHandlers[C])
        }
        .groupBy(_.serviceMethod.name)
        .map {
          case (commandName, Seq(invoker)) => commandName -> invoker
          case (commandName, many) =>
            throw new RuntimeException(
              s"Multiple methods found for handling command of name $commandName: ${many.map(_.method.getName)}"
            )
        }

    (getHandlers[CommandContext](false), getHandlers[StreamedCommandContext[AnyRef]](true))
  }

  override def create(context: ReplicatedEntityCreationContext): ReplicatedEntityHandler = {
    val entity = constructor(context)
    new EntityHandler(entity)
  }

  private class EntityHandler(entity: AnyRef) extends ReplicatedEntityHandler {

    override def handleCommand(command: JavaPbAny, context: CommandContext): Reply[JavaPbAny] = unwrap {
      val maybeResult = commandHandlers.get(context.commandName()).map { handler =>
        handler.invoke(entity, command, context)
      }

      maybeResult.getOrElse {
        throw new RuntimeException(
          s"No command handler found for command [${context.commandName()}] on Replicated Entity: $entityClass"
        )
      }
    }

    override def handleStreamedCommand(command: JavaPbAny,
                                       context: StreamedCommandContext[JavaPbAny]): Reply[JavaPbAny] = unwrap {
      val maybeResult = streamedCommandHandlers.get(context.commandName()).map { handler =>
        val adaptedContext =
          new AdaptedStreamedCommandContext(context,
                                            handler.serviceMethod.outputType.asInstanceOf[ResolvedType[AnyRef]])
        handler.invoke(entity, command, adaptedContext)
      }

      maybeResult.getOrElse {
        throw new RuntimeException(
          s"No streamed command handler found for command [${context.commandName()}] on Replicated Entity: $entityClass"
        )
      }
    }

    private def unwrap[T](block: => T): T =
      try {
        block
      } catch {
        case ite: InvocationTargetException if ite.getCause != null =>
          throw ite.getCause
      }
  }

}

private object ReplicatedEntityAnnotationHelper {
  private case class ReplicatedEntityInjector[C <: ReplicatedData, T](replicatedDataType: Class[C],
                                                                      create: ReplicatedDataFactory => T,
                                                                      wrap: C => T)
  private def simple[D <: ReplicatedData: ClassTag](create: ReplicatedDataFactory => D)() = {
    val clazz = implicitly[ClassTag[D]].runtimeClass.asInstanceOf[Class[D]]
    clazz -> ReplicatedEntityInjector[D, D](clazz, create, identity)
      .asInstanceOf[ReplicatedEntityInjector[ReplicatedData, AnyRef]]
  }
  private def orMapWrapper[W: ClassTag, D <: ReplicatedData](wrap: ORMap[AnyRef, D] => W) =
    implicitly[ClassTag[W]].runtimeClass
      .asInstanceOf[Class[D]] -> ReplicatedEntityInjector(classOf[ORMap[AnyRef, D]], f => wrap(f.newORMap()), wrap)
      .asInstanceOf[ReplicatedEntityInjector[ReplicatedData, AnyRef]]

  private val injectorMap: Map[Class[_], ReplicatedEntityInjector[ReplicatedData, AnyRef]] = Map(
    simple(_.newCounter()),
    simple(_.newReplicatedSet()),
    simple(_.newRegister()),
    simple(_.newORMap()),
    simple(_.newVote()),
    orMapWrapper[ReplicatedRegisterMap[AnyRef, AnyRef], ReplicatedRegister[AnyRef]](new ReplicatedRegisterMap(_)),
    orMapWrapper[ReplicatedCounterMap[AnyRef], ReplicatedCounter](new ReplicatedCounterMap(_))
  )

  private def injector[D <: ReplicatedData, T](clazz: Class[T]): ReplicatedEntityInjector[D, T] =
    injectorMap.get(clazz) match {
      case Some(injector: ReplicatedEntityInjector[D, T] @unchecked) => injector
      case None => throw new RuntimeException(s"Don't know how to inject Replicated Data of type $clazz")
    }

  def replicatedEntityParameterHandlers[C <: ReplicatedEntityContext with ReplicatedDataFactory]
      : PartialFunction[MethodParameter, ParameterHandler[AnyRef, C]] = {
    case methodParam if injectorMap.contains(methodParam.parameterType) =>
      new ReplicatedEntityParameterHandler[C, ReplicatedData, AnyRef](injectorMap(methodParam.parameterType),
                                                                      methodParam.method)
    case methodParam
        if methodParam.parameterType == classOf[Optional[_]] &&
        injectorMap.contains(ReflectionHelper.getFirstParameter(methodParam.genericParameterType)) =>
      new OptionalReplicatedDataParameterHandler(
        injectorMap(ReflectionHelper.getFirstParameter(methodParam.genericParameterType)),
        methodParam.method
      )
  }

  private class ReplicatedEntityParameterHandler[C <: ReplicatedEntityContext with ReplicatedDataFactory,
                                                 D <: ReplicatedData,
                                                 T](
      injector: ReplicatedEntityInjector[D, T],
      method: Executable
  ) extends ParameterHandler[AnyRef, C] {
    override def apply(ctx: InvocationContext[AnyRef, C]): AnyRef = {
      val replicatedData = ctx.context.state(injector.replicatedDataType)
      if (replicatedData.isPresent) {
        injector.wrap(replicatedData.get()).asInstanceOf[AnyRef]
      } else {
        injector.create(ctx.context).asInstanceOf[AnyRef]
      }
    }
  }

  private class OptionalReplicatedDataParameterHandler[D <: ReplicatedData, T](injector: ReplicatedEntityInjector[D, T],
                                                                               method: Executable)
      extends ParameterHandler[AnyRef, ReplicatedEntityContext] {

    import scala.compat.java8.OptionConverters._
    override def apply(ctx: InvocationContext[AnyRef, ReplicatedEntityContext]): AnyRef =
      ctx.context.state(injector.replicatedDataType).asScala.map(injector.wrap).asJava
  }

}

private final class AdaptedStreamedCommandContext(val delegate: StreamedCommandContext[JavaPbAny],
                                                  resolvedType: ResolvedType[AnyRef])
    extends StreamedCommandContext[AnyRef] {
  override def isStreamed: Boolean = delegate.isStreamed

  def onChange(subscriber: function.Function[SubscriptionContext, Optional[AnyRef]]): Unit =
    delegate.onChange { ctx =>
      val result = subscriber(ctx)
      if (result.isPresent) {
        Optional.of(
          JavaPbAny
            .newBuilder()
            .setTypeUrl(resolvedType.typeUrl)
            .setValue(resolvedType.toByteString(result.get))
            .build()
        )
      } else {
        Optional.empty()
      }
    }

  override def onCancel(effect: Consumer[StreamCancelledContext]): Unit = delegate.onCancel(effect)

  override def serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()
  override def entityId(): String = delegate.entityId()
  override def commandId(): Long = delegate.commandId()
  override def commandName(): String = delegate.commandName()
  override def metadata(): Metadata = delegate.metadata()

  override def state[D <: ReplicatedData](dataClass: Class[D]): Optional[D] = delegate.state(dataClass)
  override def delete(): Unit = delegate.delete()

  override def forward(to: ServiceCall): Unit = delegate.forward(to)
  override def fail(errorMessage: String): RuntimeException = delegate.fail(errorMessage)
  override def effect(effect: ServiceCall, synchronous: Boolean): Unit = delegate.effect(effect, synchronous)

  override def newCounter(): ReplicatedCounter = delegate.newCounter()
  override def newReplicatedSet[T](): ReplicatedSet[T] = delegate.newReplicatedSet()
  override def newRegister[T](value: T): ReplicatedRegister[T] = delegate.newRegister(value)
  override def newORMap[K, V <: ReplicatedData](): ORMap[K, V] = delegate.newORMap()
  override def newVote(): Vote = delegate.newVote()

  override def getWriteConsistency: WriteConsistency = delegate.getWriteConsistency
  override def setWriteConsistency(consistency: WriteConsistency): Unit = delegate.setWriteConsistency(consistency)
}

private final class EntityConstructorInvoker(constructor: Constructor[_])
    extends (ReplicatedEntityCreationContext => AnyRef) {
  private val parameters =
    ReflectionHelper.getParameterHandlers[AnyRef, ReplicatedEntityCreationContext](constructor)(
      ReplicatedEntityAnnotationHelper.replicatedEntityParameterHandlers
    )
  parameters.foreach {
    case MainArgumentParameterHandler(clazz) =>
      throw new RuntimeException(s"Don't know how to handle argument of type ${clazz.getName} in constructor")
    case _ =>
  }

  def apply(context: ReplicatedEntityCreationContext): AnyRef = {
    val ctx = InvocationContext(null.asInstanceOf[AnyRef], context)
    constructor.newInstance(parameters.map(_.apply(ctx)): _*).asInstanceOf[AnyRef]
  }
}
