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

package com.akkaserverless.javasdk.impl.eventsourcedentity

import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase.Effect
import com.akkaserverless.javasdk.eventsourcedentity._
import com.akkaserverless.javasdk.impl.EntityExceptions.EntityException
import com.akkaserverless.javasdk.impl.FailInvoked
import com.akkaserverless.javasdk.impl.ReflectionHelper.MethodParameter
import com.akkaserverless.javasdk.impl.ReflectionHelper.ParameterHandler
import com.akkaserverless.javasdk.impl.ReflectionHelper.InvocationContext
import com.akkaserverless.javasdk.impl.ReflectionHelper.MainArgumentParameterHandler
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.impl.ReflectionHelper
import com.akkaserverless.javasdk.impl.ResolvedEntityFactory
import com.akkaserverless.javasdk.impl.ResolvedServiceMethod
import com.akkaserverless.javasdk.EntityFactory
import com.akkaserverless.javasdk.ServiceCallFactory
import com.akkaserverless.javasdk.lowlevel.EventSourcedEntityFactory
import com.google.protobuf.Descriptors
import com.google.protobuf.{Any => JavaPbAny}

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Optional
import scala.collection.concurrent.TrieMap

/**
 * Annotation based implementation of the [[EventSourcedEntityFactory]].
 */
private[impl] class AnnotationBasedEventSourcedSupport(
    entityClass: Class[_],
    anySupport: AnySupport,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]],
    factory: Option[EventSourcedEntityCreationContext => EventSourcedEntityBase[_]] = None
) extends EventSourcedEntityFactory
    with ResolvedEntityFactory {

  def this(entityClass: Class[_], anySupport: AnySupport, serviceDescriptor: Descriptors.ServiceDescriptor) =
    this(entityClass, anySupport, anySupport.resolveServiceDescriptor(serviceDescriptor))

  def this(factory: EntityFactory, anySupport: AnySupport, serviceDescriptor: Descriptors.ServiceDescriptor) =
    this(factory.entityClass,
         anySupport,
         anySupport.resolveServiceDescriptor(serviceDescriptor),
         Some(context => factory.create(context).asInstanceOf[EventSourcedEntityBase[_]]))

  private val behavior = EventBehaviorReflection(entityClass, resolvedMethods, anySupport)

  private val constructor: EventSourcedEntityCreationContext => EventSourcedEntityBase[_] = factory.getOrElse {
    entityClass.getConstructors match {
      case Array(single) =>
        new EntityConstructorInvoker(ReflectionHelper.ensureAccessible(single))
      case _ =>
        throw new RuntimeException(s"Only a single constructor is allowed on event sourced entities: $entityClass")
    }
  }

  override def create(context: EventSourcedContext): EventSourcedEntityHandler[_, _] =
    new EntityHandler({
      constructor(new DelegatingEventSourcedContext(context) with EventSourcedEntityCreationContext {
        override def entityId(): String = context.entityId()
      })
    })

  private class EntityHandler[S](entity: EventSourcedEntityBase[S])
      extends EventSourcedEntityHandler[S, EventSourcedEntityBase[S]](entity) {

    override def handleEvent(state: S, event: Any): S = unwrap {
      behavior.getCachedEventHandlerForClass(event.getClass) match {
        case Some(handler) =>
          handler.invoke(entity, stateOrEmpty(), event.asInstanceOf[AnyRef]).asInstanceOf[S]
        case None =>
          throw EntityException(
            s"No event handler found for event ${event.getClass} on $behaviorsString"
          )
      }
    }

    override def handleCommand(commandName: String,
                               state: S,
                               command: JavaPbAny,
                               context: CommandContext): EventSourcedEntityBase.Effect[Any] =
      unwrap {
        behavior.commandHandlers.get(commandName).map { handler =>
          handler.commandInvoke(entity, stateOrEmpty(), command).asInstanceOf[Effect[Any]]
        } getOrElse {
          throw new RuntimeException(
            s"No command handler found for command [${commandName}] on $behaviorsString"
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

  private abstract class DelegatingEventSourcedContext(delegate: EventSourcedContext) extends EventSourcedContext {
    override def entityId(): String = delegate.entityId()
    override def serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()
  }
}

private class EventBehaviorReflection(
    eventHandlers: Map[Class[_], EventHandlerInvoker],
    val commandHandlers: Map[String, EventSourcedCommandHandlerInvoker]
) {

  /**
   * We use a cache in addition to the info we've discovered by reflection so that an event handler can be declared
   * for a superclass of an event.
   */
  private val eventHandlerCache = TrieMap.empty[Class[_], Option[EventHandlerInvoker]]

  def getCachedEventHandlerForClass(clazz: Class[_]): Option[EventHandlerInvoker] =
    eventHandlerCache.getOrElseUpdate(clazz, getHandlerForClass(eventHandlers)(clazz))

  private def getHandlerForClass[T](handlers: Map[Class[_], T])(clazz: Class[_]): Option[T] =
    handlers.get(clazz) match {
      case some @ Some(_) => some
      case None =>
        clazz.getInterfaces.collectFirst(Function.unlift(getHandlerForClass(handlers))) match {
          case some @ Some(_) => some
          case None if clazz.getSuperclass != null => getHandlerForClass(handlers)(clazz.getSuperclass)
          case None => None
        }
    }

}

private object EventBehaviorReflection {
  def apply(behaviorClass: Class[_],
            serviceMethods: Map[String, ResolvedServiceMethod[_, _]],
            anySupport: AnySupport): EventBehaviorReflection = {

    val allMethods = ReflectionHelper.getAllDeclaredMethods(behaviorClass)
    val eventHandlers = allMethods
      .filter(_.getAnnotation(classOf[EventHandler]) != null)
      .map { method =>
        new EventHandlerInvoker(ReflectionHelper.ensureAccessible(method), anySupport)
      }
      .groupBy(_.eventClass)
      .map {
        case (eventClass, Seq(invoker)) => (eventClass: Any) -> invoker
        case (clazz, many) =>
          throw new RuntimeException(
            s"Multiple methods found for handling event of type $clazz: ${many.map(_.method.getName)}"
          )
      }
      .asInstanceOf[Map[Class[_], EventHandlerInvoker]]

    val commandHandlers = allMethods
      .filter(_.getAnnotation(classOf[CommandHandler]) != null)
      .map { method =>
        val annotation = method.getAnnotation(classOf[CommandHandler])
        val name: String = if (annotation.name().isEmpty) {
          ReflectionHelper.getCapitalizedName(method)
        } else annotation.name()

        val serviceMethod = serviceMethods.getOrElse(name, {
          throw new RuntimeException(
            s"Command handler method ${method.getName} for command $name found, but the service has no command by that name."
          )
        })

        new EventSourcedCommandHandlerInvoker(ReflectionHelper.ensureAccessible(method), serviceMethod, anySupport)
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
      classOf[EventSourcedEntity],
      Set(classOf[EventHandler], classOf[CommandHandler])
    )

    new EventBehaviorReflection(eventHandlers, commandHandlers)
  }
}

private class EntityConstructorInvoker(constructor: Constructor[_])
    extends (EventSourcedEntityCreationContext => EventSourcedEntityBase[_]) {
  private val parameters =
    ReflectionHelper.getParameterHandlers[AnyRef, EventSourcedEntityCreationContext](constructor)()
  parameters.foreach {
    case MainArgumentParameterHandler(clazz) =>
      throw new RuntimeException(s"Don't know how to handle argument of type ${clazz.getName} in constructor")
    case _ =>
  }

  def apply(context: EventSourcedEntityCreationContext): EventSourcedEntityBase[_] = {
    val ctx = InvocationContext(null.asInstanceOf[AnyRef], context)
    constructor.newInstance(parameters.map(_.apply(ctx)): _*).asInstanceOf[EventSourcedEntityBase[_]]
  }
}

private class EventHandlerInvoker(val method: Method, anySupport: AnySupport) {

  private val annotation = method.getAnnotation(classOf[EventHandler])

  private val parameters = ReflectionHelper.getParameterHandlers[AnyRef, EventContext](method)()

  private def annotationEventClass = annotation.eventClass() match {
    case obj if obj == classOf[Object] => None
    case clazz => Some(clazz)
  }

  // Verify that there is at most one event handler
  val eventClass: Class[_] = parameters.collect {
    case MainArgumentParameterHandler(clazz) => clazz
  } match {
    case Array() => annotationEventClass.getOrElse(classOf[Object])
    case Array(stateClass, eventClass) =>
      annotationEventClass match {
        case None => eventClass
        case Some(annotated) if eventClass.isAssignableFrom(annotated) || annotated.isInterface =>
          annotated
        case Some(nonAssignable) =>
          throw new RuntimeException(
            s"EventHandler method $method has defined an eventHandler class $nonAssignable that can never be assignable from it's parameter $eventClass"
          )
      }
    case other =>
      throw new RuntimeException(
        s"EventHandler method $method must define at most one non context parameter to handle events, the parameters defined were: ${other
          .mkString(",")}"
      )
  }

  def invoke(obj: AnyRef, state: Any, event: AnyRef): Object = {
    method.invoke(obj, state, event)
  }
}

private class EventSourcedCommandHandlerInvoker(
    method: Method,
    serviceMethod: ResolvedServiceMethod[_, _],
    anySupport: AnySupport,
    extraParameters: PartialFunction[MethodParameter, ParameterHandler[AnyRef, CommandContext]] = PartialFunction.empty
) extends ReflectionHelper.CommandHandlerInvoker[CommandContext](method, serviceMethod, anySupport, extraParameters) {

  def commandInvoke(obj: AnyRef, state: Any, command: JavaPbAny): EventSourcedEntityEffectImpl[Any] = {
    val decodedCommand = mainArgumentDecoder(command)
    try {
      method.invoke(obj, state, decodedCommand) match {
        case effect: EventSourcedEntityEffectImpl[_] => effect.asInstanceOf[EventSourcedEntityEffectImpl[Any]]
        case null =>
          throw new NullPointerException(s"${method} returned null")
        case _ =>
          throw new IllegalStateException(s"${method} should return an effect now")
      }
    } catch {
      case e: InvocationTargetException =>
        e.getCause match {
          case FailInvoked => throw e
          case x => throw x
        }
    }
  }
}
