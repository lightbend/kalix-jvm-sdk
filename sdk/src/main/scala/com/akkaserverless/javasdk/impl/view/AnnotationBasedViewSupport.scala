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

package com.akkaserverless.javasdk.impl.view

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.Optional
import com.akkaserverless.javasdk.{Reply, ServiceCallFactory}
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.impl.ReflectionHelper
import com.akkaserverless.javasdk.impl.ReflectionHelper.InvocationContext
import com.akkaserverless.javasdk.impl.ReflectionHelper.MainArgumentParameterHandler
import com.akkaserverless.javasdk.impl.ReflectionHelper.MethodParameter
import com.akkaserverless.javasdk.impl.ReflectionHelper.ParameterHandler
import com.akkaserverless.javasdk.impl.ResolvedEntityFactory
import com.akkaserverless.javasdk.impl.ResolvedServiceMethod
import com.akkaserverless.javasdk.view.UpdateHandler
import com.akkaserverless.javasdk.view.UpdateHandlerContext
import com.akkaserverless.javasdk.view.View
import com.akkaserverless.javasdk.view.ViewContext
import com.akkaserverless.javasdk.view.ViewCreationContext
import com.akkaserverless.javasdk.view.ViewFactory
import com.akkaserverless.javasdk.view.ViewUpdateHandler
import com.google.protobuf.Descriptors
import com.google.protobuf.{Any => JavaPbAny}

/**
 * INTERNAL API
 *
 * Annotation based implementation of the [[ViewFactory]].
 */
private[impl] class AnnotationBasedViewSupport(
    viewClass: Class[_],
    anySupport: AnySupport,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]],
    factory: Option[ViewCreationContext => AnyRef]
) extends ViewFactory
    with ResolvedEntityFactory {

  def this(viewClass: Class[_], anySupport: AnySupport, serviceDescriptor: Descriptors.ServiceDescriptor) =
    this(viewClass, anySupport, anySupport.resolveServiceDescriptor(serviceDescriptor), None)

  private val behavior = ViewBehaviorReflection(viewClass, resolvedMethods, anySupport)

  private val constructor: ViewCreationContext => AnyRef = factory.getOrElse {
    viewClass.getConstructors match {
      case Array(single) =>
        new ViewConstructorInvoker(ReflectionHelper.ensureAccessible(single))
      case _ =>
        throw new RuntimeException(s"Only a single constructor is allowed on views: $viewClass")
    }
  }

  override def create(context: ViewContext): ViewUpdateHandler =
    new ViewUpdateHandlerImpl(context)

  private class ViewUpdateHandlerImpl(context: ViewContext) extends ViewUpdateHandler {
    private val view = {
      constructor(new DelegatingViewContext(context) with ViewCreationContext)
    }

    override def handle(message: JavaPbAny, context: UpdateHandlerContext): Reply[JavaPbAny] = unwrap {
      behavior.commandHandlers.get(context.commandName()).map { handler =>
        handler.invoke(view, message, context)
      } getOrElse {
        throw ViewException(context,
                            s"No handler found for command [${context.commandName()}] on $behaviorsString",
                            None)
      }
    }

    private def unwrap[T](block: => T): T =
      try {
        block
      } catch {
        case ite: InvocationTargetException if ite.getCause != null =>
          throw ite.getCause
      }

    private def behaviorsString = view.getClass.toString
  }

  private class DelegatingViewContext(delegate: ViewContext) extends ViewContext {
    override def viewId(): String = delegate.viewId()
    override def serviceCallFactory(): ServiceCallFactory = delegate.serviceCallFactory()
  }
}

private class ViewBehaviorReflection(
    val commandHandlers: Map[String, ReflectionHelper.CommandHandlerInvoker[UpdateHandlerContext]]
)

private object ViewBehaviorReflection {
  def apply(behaviorClass: Class[_],
            serviceMethods: Map[String, ResolvedServiceMethod[_, _]],
            anySupport: AnySupport): ViewBehaviorReflection = {

    val allMethods = ReflectionHelper.getAllDeclaredMethods(behaviorClass)
    val commandHandlers = allMethods
      .collect {
        case method if method.getAnnotation(classOf[UpdateHandler]) != null =>
          val annotation = method.getAnnotation(classOf[UpdateHandler])
          val name: String = if (annotation.name().isEmpty) {
            ReflectionHelper.getCapitalizedName(method)
          } else annotation.name()

          val serviceMethod = serviceMethods.getOrElse(name, {
            throw new RuntimeException(
              s"Command handler method ${method.getName} for command $name found, but the service has no command with that name."
            )
          })

          def stateParameterHandlers
              : PartialFunction[MethodParameter, ParameterHandler[AnyRef, UpdateHandlerContext]] = {
            case param
                if param.param > 0 // message (main arg) must be first param
                && param.parameterType == serviceMethod.outputType.typeClass =>
              // note that the serviceMethod.outputType is used
              val decoder = ReflectionHelper.getMainArgumentDecoder(serviceMethod.name,
                                                                    param.parameterType.asInstanceOf[Class[AnyRef]],
                                                                    serviceMethod.outputType)
              new StateParameterHandler(decoder, optional = false)

            case param
                if param.param > 0 // message (main arg) must be first param
                && param.parameterType == classOf[Optional[_]]
                && ReflectionHelper.getFirstParameter(param.genericParameterType) == serviceMethod.outputType.typeClass =>
              // note that the serviceMethod.outputType is used
              val stateClass = ReflectionHelper.getFirstParameter(param.genericParameterType)
              val decoder =
                ReflectionHelper.getMainArgumentDecoder(serviceMethod.name, stateClass, serviceMethod.outputType)
              new StateParameterHandler(decoder, optional = true)
          }

          new ReflectionHelper.CommandHandlerInvoker[UpdateHandlerContext](ReflectionHelper.ensureAccessible(method),
                                                                           serviceMethod,
                                                                           anySupport,
                                                                           stateParameterHandlers)
      }
      .groupBy(_.serviceMethod.name)
      .map {
        case (commandName, Seq(invoker)) => commandName -> invoker
        case (commandName, many) =>
          throw new RuntimeException(
            s"Multiple methods found for handling message of name $commandName: ${many.map(_.method.getName)}"
          )
      }

    ReflectionHelper.validateNoBadMethods(
      allMethods,
      classOf[View],
      Set(classOf[UpdateHandler])
    )

    new ViewBehaviorReflection(commandHandlers)
  }

  private class StateParameterHandler(decoder: JavaPbAny => AnyRef, optional: Boolean)
      extends ParameterHandler[AnyRef, UpdateHandlerContext] {
    override def apply(ctx: InvocationContext[AnyRef, UpdateHandlerContext]): AnyRef = {
      val ctxState = ctx.context match {
        case stateCtx: StateContext => stateCtx.state
        case other => throw new IllegalStateException(s"Expected StateContext: $other")
      }
      val decodedState = {
        if (ctxState.isPresent) decoder(ctxState.get)
        else null
      }
      if (optional) Optional.ofNullable(decodedState)
      else decodedState
    }
  }

}

private class ViewConstructorInvoker(constructor: Constructor[_]) extends (ViewCreationContext => AnyRef) {
  private val parameters = ReflectionHelper.getParameterHandlers[AnyRef, ViewCreationContext](constructor)()
  parameters.foreach {
    case MainArgumentParameterHandler(clazz) =>
      throw new RuntimeException(s"Don't know how to handle argument of type ${clazz.getName} in constructor")
    case _ =>
  }

  def apply(context: ViewCreationContext): AnyRef = {
    val ctx = InvocationContext(null.asInstanceOf[AnyRef], context)
    constructor.newInstance(parameters.map(_.apply(ctx)): _*).asInstanceOf[AnyRef]
  }
}
