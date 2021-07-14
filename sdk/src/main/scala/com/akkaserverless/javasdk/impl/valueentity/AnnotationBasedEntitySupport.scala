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

package com.akkaserverless.javasdk.impl.valueentity

import com.akkaserverless.javasdk.valueentity.ValueEntityContext
import com.akkaserverless.javasdk.impl.EntityExceptions.EntityException
import com.akkaserverless.javasdk.impl.ReflectionHelper.{
  getFirstParameter,
  getOutputParameterMapper,
  InvocationContext,
  MainArgumentParameterHandler,
  MethodParameter,
  ParameterHandler
}
import com.akkaserverless.javasdk.impl.effect.{
  ErrorReplyImpl,
  ForwardReplyImpl,
  MessageReplyImpl,
  NoReply,
  NoSecondaryEffectImpl
}
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl.UpdateState
import com.akkaserverless.javasdk.impl.{
  AnySupport,
  FailInvoked,
  ReflectionHelper,
  ResolvedEntityFactory,
  ResolvedServiceMethod
}
import com.akkaserverless.javasdk.{Metadata, Reply, ServiceCall, ServiceCallFactory}
import com.google.protobuf.{Descriptors, any, Any => JavaPbAny}

import java.lang.reflect.{Constructor, InvocationTargetException, Method}
import java.util.Optional
import com.akkaserverless.javasdk.lowlevel.ValueEntityFactory
import com.akkaserverless.javasdk.lowlevel.ValueEntityHandler
import com.akkaserverless.javasdk.valueentity._

/**
 * Annotation based implementation of the [[EntityFactory]].
 */
private[impl] class AnnotationBasedEntitySupport(
    entityClass: Class[_],
    anySupport: AnySupport,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]],
    factory: Option[ValueEntityCreationContext => AnyRef] = None
) extends ValueEntityFactory
    with ResolvedEntityFactory {

  def this(entityClass: Class[_], anySupport: AnySupport, serviceDescriptor: Descriptors.ServiceDescriptor) =
    this(entityClass, anySupport, anySupport.resolveServiceDescriptor(serviceDescriptor))

  private val behavior = EntityBehaviorReflection(entityClass, resolvedMethods, anySupport)

  private val constructor: ValueEntityCreationContext => AnyRef = factory.getOrElse {
    entityClass.getConstructors match {
      case Array(single) =>
        new EntityConstructorInvoker(ReflectionHelper.ensureAccessible(single))
      case _ =>
        throw new RuntimeException(s"Only a single constructor is allowed on value-based entities: $entityClass")
    }
  }

  override def create(context: ValueEntityContext): ValueEntityHandler =
    new EntityHandler(context)

  private class EntityHandler(context: ValueEntityContext) extends ValueEntityHandler {
    private val entity = {
      constructor(new DelegatingEntityContext(context) with ValueEntityCreationContext {
        override def entityId(): String = context.entityId()
      })
    }

    // FIXME the annotation-based implementation is going away before we release anyway
    override def emptyState(): any.Any = null

    override def handleCommand(command: JavaPbAny,
                               state: JavaPbAny,
                               context: CommandContext[JavaPbAny]): ValueEntityBase.Effect[JavaPbAny] = unwrap {
      behavior.commandHandlers.get(context.commandName()).map { handler =>
        val adaptedContext =
          new AdaptedCommandContext[AnyRef](context, anySupport)
        handler.valueEntityInvoke(entity, command, adaptedContext)
      } getOrElse {
        throw EntityException(
          context,
          s"No command handler found for command [${context.commandName()}] on $behaviorsString"
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

    private def behaviorsString = entity.getClass.toString
  }

  private abstract class DelegatingEntityContext(delegate: ValueEntityContext) extends ValueEntityContext {
    override def entityId(): String = delegate.entityId()
    override def serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()
  }
}

private class ValueEntityCommandHandlerInvoker(
    method: Method,
    serviceMethod: ResolvedServiceMethod[_, _],
    anySupport: AnySupport,
    extraParameters: PartialFunction[MethodParameter, ParameterHandler[AnyRef, CommandContext[AnyRef]]] =
      PartialFunction.empty
) extends ReflectionHelper.CommandHandlerInvoker[CommandContext[AnyRef]](method,
                                                                           serviceMethod,
                                                                           anySupport,
                                                                           extraParameters) {

  def valueEntityInvoke(obj: AnyRef,
                        command: JavaPbAny,
                        context: CommandContext[AnyRef]): ValueEntityBase.Effect[JavaPbAny] = {
    val decodedCommand = mainArgumentDecoder(command)
    val ctx = InvocationContext(decodedCommand, context)
    try {
      method.invoke(obj, parameters.map(_.apply(ctx)): _*) match {
        case effect: ValueEntityEffectImpl[_] =>
          serializePrimaryEffect(serializeSecondaryEffect(effect))
        case null =>
          throw new NullPointerException(s"${method} returned null")
        case _ =>
          throw new IllegalStateException(s"${method} should return an effect now")
      }
    } catch {
      case e: InvocationTargetException =>
        e.getCause match {
          case FailInvoked => throw e
          case x => throw e.getCause()
        }
    }
  }

  def serializePrimaryEffect(effect: ValueEntityEffectImpl[_]): ValueEntityEffectImpl[JavaPbAny] =
    effect.primaryEffect match {
      case UpdateState(newState) =>
        effect
          .asInstanceOf[ValueEntityEffectImpl[JavaPbAny]]
          .updateState(anySupport.encodeJava(newState))
          .asInstanceOf[ValueEntityEffectImpl[JavaPbAny]]
      case other => effect.asInstanceOf[ValueEntityEffectImpl[JavaPbAny]]
    }
  def serializeSecondaryEffect[T](effect: ValueEntityEffectImpl[T]): ValueEntityEffectImpl[T] =
    effect.secondaryEffect match {
      case MessageReplyImpl(message, metadata, sideEffects) =>
        effect
          .reply(serialize(message), metadata)
          .asInstanceOf[ValueEntityEffectImpl[T]]
      case other => effect
    }
}

private class EntityBehaviorReflection(
    val commandHandlers: Map[String, ValueEntityCommandHandlerInvoker]
) {}

private object EntityBehaviorReflection {
  def apply(behaviorClass: Class[_],
            serviceMethods: Map[String, ResolvedServiceMethod[_, _]],
            anySupport: AnySupport): EntityBehaviorReflection = {

    val allMethods = ReflectionHelper.getAllDeclaredMethods(behaviorClass)
    val commandHandlers = allMethods
      .filter(_.getAnnotation(classOf[CommandHandler]) != null)
      .map { method =>
        val annotation = method.getAnnotation(classOf[CommandHandler])
        val name: String = if (annotation.name().isEmpty) {
          ReflectionHelper.getCapitalizedName(method)
        } else annotation.name()

        val serviceMethod = serviceMethods.getOrElse(
          name, {
            throw new RuntimeException(
              s"Command handler method [${method.getDeclaringClass.getSimpleName}.${method.getName}] for command [$name] found, but the service has no command with that name${serviceMethods.keys
                .mkString(" (existing commands are: ", ", ", ")")}."
            )
          }
        )

        new ValueEntityCommandHandlerInvoker(ReflectionHelper.ensureAccessible(method), serviceMethod, anySupport)
      }
      .groupBy(_.serviceMethod.name)
      .map {
        case (commandName, Seq(invoker)) => commandName -> invoker
        case (commandName, many) =>
          throw new RuntimeException(
            s"Multiple methods found for handling command of name $commandName: ${many.map(_.method.getName)}"
          )
      }

    ReflectionHelper.validateNoBadMethods(
      allMethods,
      classOf[ValueEntity],
      Set(classOf[CommandHandler])
    )

    new EntityBehaviorReflection(commandHandlers)
  }
}

private class EntityConstructorInvoker(constructor: Constructor[_]) extends (ValueEntityCreationContext => AnyRef) {
  private val parameters = ReflectionHelper.getParameterHandlers[AnyRef, ValueEntityCreationContext](constructor)()
  parameters.foreach {
    case MainArgumentParameterHandler(clazz) =>
      throw new RuntimeException(s"Don't know how to handle argument of type ${clazz.getName} in constructor")
    case _ =>
  }

  def apply(context: ValueEntityCreationContext): AnyRef = {
    val ctx = InvocationContext(null.asInstanceOf[AnyRef], context)
    constructor.newInstance(parameters.map(_.apply(ctx)): _*).asInstanceOf[AnyRef]
  }
}

/*
 * This class is a conversion bridge between CommandContext[JavaPbAny] and CommandContext[AnyRef].
 * It helps for making the conversion from JavaPbAny to AnyRef and backward.
 */
private class AdaptedCommandContext[S](val delegate: CommandContext[JavaPbAny], anySupport: AnySupport)
    extends CommandContext[S] {

  override def getState(): Optional[S] = {
    val result = delegate.getState
    result.map(anySupport.decode(_).asInstanceOf[S])
  }

  override def commandName(): String = delegate.commandName()
  override def commandId(): Long = delegate.commandId()
  override def metadata(): Metadata = delegate.metadata()
  override def entityId(): String = delegate.entityId()
  override def effect(effect: ServiceCall, synchronous: Boolean): Unit = delegate.effect(effect, synchronous)
  override def fail(errorMessage: String): RuntimeException = delegate.fail(errorMessage)
  override def forward(to: ServiceCall): Unit = delegate.forward(to)
  override def serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()
}
