/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.action

import akka.NotUsed
import akka.stream.javadsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.{javadsl, Materializer}
import com.akkaserverless.javasdk.{Jsonable, Reply}
import com.akkaserverless.javasdk.action._
import com.akkaserverless.javasdk.impl.ReflectionHelper.{InvocationContext, ParameterHandler}
import com.akkaserverless.javasdk.impl._
import com.akkaserverless.javasdk.reply.MessageReply
import com.google.protobuf.{Descriptors, Any => JavaPbAny}
import java.lang.reflect.{InvocationTargetException, Method, Type}
import java.util.concurrent.{CompletableFuture, CompletionStage}

/**
 * Annotation based implementation of the [[ActionHandler]].
 */
private[impl] class AnnotationBasedActionSupport(
    action: AnyRef,
    anySupport: AnySupport,
    override val resolvedMethods: Map[String, ResolvedServiceMethod[_, _]]
)(implicit mat: Materializer)
    extends ActionHandler
    with ResolvedEntityFactory {

  def this(action: AnyRef, anySupport: AnySupport, serviceDescriptor: Descriptors.ServiceDescriptor)(
      implicit mat: Materializer
  ) =
    this(action, anySupport, anySupport.resolveServiceDescriptor(serviceDescriptor))

  private val behavior = ActionReflection(action.getClass, resolvedMethods, anySupport)

  override def handleUnary(commandName: String,
                           message: MessageEnvelope[JavaPbAny],
                           context: ActionContext): CompletionStage[Reply[JavaPbAny]] = unwrap {
    behavior.unaryHandlers.get(commandName) match {
      case Some(handler) =>
        handler.invoke(action, message, context)
      case None =>
        throw new RuntimeException(
          s"No call handler found for call $commandName on ${action.getClass.getName}"
        )
    }
  }

  override def handleStreamedOut(commandName: String,
                                 message: MessageEnvelope[JavaPbAny],
                                 context: ActionContext): Source[Reply[JavaPbAny], NotUsed] = unwrap {
    behavior.serverStreamedHandlers.get(commandName) match {
      case Some(handler) =>
        handler.invoke(action, message, context)
      case None =>
        throw new RuntimeException(
          s"No call handler found for call $commandName on ${action.getClass.getName}"
        )
    }
  }

  override def handleStreamedIn(commandName: String,
                                stream: Source[MessageEnvelope[JavaPbAny], NotUsed],
                                context: ActionContext): CompletionStage[Reply[JavaPbAny]] =
    behavior.clientStreamedHandlers.get(commandName) match {
      case Some(handler) =>
        handler.invoke(action, stream, context)
      case None =>
        throw new RuntimeException(
          s"No call handler found for call $commandName on ${action.getClass.getName}"
        )
    }

  override def handleStreamed(commandName: String,
                              stream: Source[MessageEnvelope[JavaPbAny], NotUsed],
                              context: ActionContext): Source[Reply[JavaPbAny], NotUsed] =
    behavior.streamedHandlers.get(commandName) match {
      case Some(handler) =>
        handler.invoke(action, stream, context)
      case None =>
        throw new RuntimeException(
          s"No call handler found for call $commandName on ${action.getClass.getName}"
        )
    }

  private def unwrap[T](block: => T): T =
    try {
      block
    } catch {
      case ite: InvocationTargetException if ite.getCause != null =>
        throw ite.getCause
    }
}

private class ActionReflection(
    val unaryHandlers: Map[String, UnaryCallInvoker],
    val serverStreamedHandlers: Map[String, ServerStreamedCallInvoker],
    val clientStreamedHandlers: Map[String, ClientStreamedCallInvoker],
    val streamedHandlers: Map[String, StreamedCallInvoker],
    val anySupport: AnySupport
)

private object ActionReflection {
  def apply(behaviorClass: Class[_], serviceMethods: Map[String, ResolvedServiceMethod[_, _]], anySupport: AnySupport)(
      implicit mat: Materializer
  ): ActionReflection = {

    val allMethods = ReflectionHelper.getAllDeclaredMethods(behaviorClass)

    // First, find all the call handler methods, and match them with corresponding service methods
    val allCommandHandlers = allMethods
      .collect {
        case method if method.isAnnotationPresent(classOf[Handler]) =>
          method.setAccessible(true)
          val annotation = method.getAnnotation(classOf[Handler])
          val name: String = if (annotation.name().isEmpty) {
            ReflectionHelper.getCapitalizedName(method)
          } else annotation.name()

          val serviceMethod = serviceMethods.getOrElse(
            name, {
              throw new RuntimeException(
                s"Command handler method [${method.getDeclaringClass.getSimpleName}.${method.getName}] for command [$name] found, but the service has no command by that name${serviceMethods.keys
                  .mkString(" (existing commands are: ", ", ", ")")}."
              )
            }
          )

          (method, serviceMethod)
      }
      .groupBy(_._2.name)
      .map {
        case (commandName, Seq((method, serviceMethod))) => (commandName, method, serviceMethod)
        case (commandName, many) =>
          throw new RuntimeException(
            s"Multiple methods found for handling command of name $commandName: ${many.map(_._1.getName).mkString(", ")}"
          )
      }

    val unaryCommandHandlers = allCommandHandlers.collect {
      case (commandName, method, serviceMethod)
          if !serviceMethod.descriptor.isClientStreaming && !serviceMethod.descriptor.isServerStreaming =>
        commandName -> new UnaryCallInvoker(method, serviceMethod, anySupport)
    }.toMap

    val serverStreamedCommandHandlers = allCommandHandlers.collect {
      case (commandName, method, serviceMethod)
          if !serviceMethod.descriptor.isClientStreaming && serviceMethod.descriptor.isServerStreaming =>
        commandName -> new ServerStreamedCallInvoker(method, serviceMethod, anySupport)
    }.toMap

    val clientStreamedCommandHandlers = allCommandHandlers.collect {
      case (commandName, method, serviceMethod)
          if serviceMethod.descriptor.isClientStreaming && !serviceMethod.descriptor.isServerStreaming =>
        commandName -> new ClientStreamedCallInvoker(method, serviceMethod, mat, anySupport)
    }.toMap

    val streamedCommandHandlers = allCommandHandlers.collect {
      case (commandName, method, serviceMethod)
          if serviceMethod.descriptor.isClientStreaming && serviceMethod.descriptor.isServerStreaming =>
        commandName -> new StreamedCallInvoker(method, serviceMethod, mat, anySupport)
    }.toMap

    ReflectionHelper.validateNoBadMethods(
      allMethods,
      classOf[Action],
      Set(classOf[Handler])
    )

    new ActionReflection(unaryCommandHandlers,
                         serverStreamedCommandHandlers,
                         clientStreamedCommandHandlers,
                         streamedCommandHandlers,
                         anySupport)
  }

  def getOutputParameterMapper[T](method: String,
                                  resolvedType: ResolvedType[T],
                                  returnType: Type,
                                  anySupport: AnySupport): Any => Reply[JavaPbAny] =
    ReflectionHelper.getOutputParameterMapper(
      method,
      resolvedType,
      returnType,
      anySupport,
      specialMappers = {
        case envelope if envelope == classOf[MessageEnvelope[_]] =>
          val payload = ReflectionHelper.getFirstParameter(returnType)
          (payload, { any: Any =>
            val envelope = any.asInstanceOf[MessageEnvelope[T]]
            Reply.message(ReflectionHelper.serialize(resolvedType, envelope.payload), envelope.metadata)
          })
      }
    )

  def getInputParameterMapper(method: String,
                              resolvedType: ResolvedType[_],
                              parameterType: Type): MessageEnvelope[JavaPbAny] => AnyRef =
    ReflectionHelper.getRawType(parameterType) match {
      case envelope if envelope == classOf[MessageEnvelope[_]] =>
        val messageType = ReflectionHelper.getFirstParameter(parameterType)
        val decoder = ReflectionHelper.getMainArgumentDecoder(method, messageType, resolvedType)

        { envelope =>
          MessageEnvelope.of(
            decoder(envelope.payload),
            envelope.metadata
          )
        }
      case payload =>
        val decoder = ReflectionHelper.getMainArgumentDecoder(method, payload, resolvedType)

        { envelope =>
          decoder(envelope.payload)
        }
    }
}

private class PayloadParameterHandler(mapper: MessageEnvelope[JavaPbAny] => AnyRef)
    extends ParameterHandler[MessageEnvelope[JavaPbAny], ActionContext] {
  override def apply(ctx: InvocationContext[MessageEnvelope[JavaPbAny], ActionContext]): AnyRef =
    mapper(ctx.mainArgument)
}

private class StreamedPayloadParameterHandler(mapper: javadsl.Source[MessageEnvelope[JavaPbAny], NotUsed] => AnyRef)
    extends ParameterHandler[javadsl.Source[MessageEnvelope[JavaPbAny], NotUsed], ActionContext] {
  override def apply(
      ctx: InvocationContext[javadsl.Source[MessageEnvelope[JavaPbAny], NotUsed], ActionContext]
  ): AnyRef =
    mapper(ctx.mainArgument)
}

private trait UnaryInSupport {
  protected val method: Method
  protected val serviceMethod: ResolvedServiceMethod[_, _]

  protected val parameters: Array[ParameterHandler[MessageEnvelope[JavaPbAny], ActionContext]] =
    ReflectionHelper.getParameterHandlers[MessageEnvelope[JavaPbAny], ActionContext](method) {
      case payload =>
        new PayloadParameterHandler(
          ActionReflection
            .getInputParameterMapper(serviceMethod.name, serviceMethod.inputType, payload.genericParameterType)
        )
    }
}

private trait UnaryOutSupport {
  protected val method: Method
  protected val serviceMethod: ResolvedServiceMethod[_, _]
  protected val anySupport: AnySupport

  protected val outputMapper: Any => CompletionStage[Reply[JavaPbAny]] = method.getReturnType match {
    case cstage if cstage == classOf[CompletionStage[_]] =>
      val cstageType = ReflectionHelper.getGenericFirstParameter(method.getGenericReturnType)
      val mapper =
        ActionReflection.getOutputParameterMapper(serviceMethod.name, serviceMethod.outputType, cstageType, anySupport)
      any: Any => any.asInstanceOf[CompletionStage[Any]].thenApply(mapper.apply(_))

    case _ =>
      val mapper = ActionReflection.getOutputParameterMapper(serviceMethod.name,
                                                             serviceMethod.outputType,
                                                             method.getGenericReturnType,
                                                             anySupport)
      any: Any => CompletableFuture.completedFuture(mapper(any))
  }
}

private trait StreamedInSupport {
  protected val method: Method
  protected val serviceMethod: ResolvedServiceMethod[_, _]
  implicit protected val materializer: Materializer

  protected val parameters
      : Array[ParameterHandler[javadsl.Source[MessageEnvelope[JavaPbAny], NotUsed], ActionContext]] =
    ReflectionHelper.getParameterHandlers[javadsl.Source[MessageEnvelope[JavaPbAny], NotUsed], ActionContext](
      method
    ) {
      case source if source.parameterType == classOf[javadsl.Source[_, _]] =>
        val sourceType = ReflectionHelper.getGenericFirstParameter(source.genericParameterType)
        val mapper =
          ActionReflection.getInputParameterMapper(serviceMethod.name, serviceMethod.inputType, sourceType)

        new StreamedPayloadParameterHandler(source => source.map(mapper.apply(_)))

      case rsPublisher if rsPublisher.parameterType == classOf[org.reactivestreams.Publisher[_]] =>
        val publisherType = ReflectionHelper.getGenericFirstParameter(rsPublisher.genericParameterType)
        val mapper =
          ActionReflection.getInputParameterMapper(serviceMethod.name, serviceMethod.inputType, publisherType)

        new StreamedPayloadParameterHandler(
          source =>
            source.asScala
              .map(mapper.apply)
              .runWith(Sink.asPublisher(false))
        )

      case other =>
        throw new RuntimeException(
          s"Unknown input parameter of type $other. Streamed call ${serviceMethod.name} must accept a ${classOf[
            javadsl.Source[_, _]
          ]} or ${classOf[org.reactivestreams.Publisher[_]]}."
        )
    }

  if (parameters.count(_.isInstanceOf[StreamedPayloadParameterHandler]) != 1) {
    throw new RuntimeException(
      s"Streamed call ${serviceMethod.name} must accept exactly one parameter of type ${classOf[javadsl.Source[_, _]]} or ${classOf[org.reactivestreams.Publisher[_]]}"
    )
  }
}

private trait StreamedOutSupport {
  protected val method: Method
  protected val serviceMethod: ResolvedServiceMethod[_, _]
  protected val anySupport: AnySupport

  protected val outputMapper: Any => javadsl.Source[Reply[JavaPbAny], NotUsed] = method.getReturnType match {
    case source if source == classOf[javadsl.Source[_, _]] =>
      val sourceType = ReflectionHelper.getGenericFirstParameter(method.getGenericReturnType)
      val mapper: Any => Reply[JavaPbAny] =
        ActionReflection.getOutputParameterMapper(serviceMethod.name, serviceMethod.outputType, sourceType, anySupport)

      any: Any =>
        any
          .asInstanceOf[javadsl.Source[Any, _]]
          .map(mapper.apply(_))
          .mapMaterializedValue(_ => NotUsed)

    case rsPublisher if rsPublisher == classOf[org.reactivestreams.Publisher[_]] =>
      val sourceType = ReflectionHelper.getGenericFirstParameter(method.getGenericReturnType)
      val mapper: Any => Reply[JavaPbAny] =
        ActionReflection.getOutputParameterMapper(serviceMethod.name, serviceMethod.outputType, sourceType, anySupport)

      any: Any => {
        javadsl.Source
          .fromPublisher(any.asInstanceOf[org.reactivestreams.Publisher[Any]])
          .map(mapper.apply(_))
      }

    case _ =>
      throw new RuntimeException(
        s"Streamed call ${serviceMethod.name} must return a ${classOf[javadsl.Source[_, _]]} or ${classOf[org.reactivestreams.Publisher[_]]}."
      )
  }
}

private class UnaryCallInvoker(protected val method: Method,
                               protected val serviceMethod: ResolvedServiceMethod[_, _],
                               protected val anySupport: AnySupport)
    extends UnaryInSupport
    with UnaryOutSupport {

  def invoke(action: AnyRef,
             message: MessageEnvelope[JavaPbAny],
             context: ActionContext): CompletionStage[Reply[JavaPbAny]] = {
    val ctx = InvocationContext(message, context)
    val result = method.invoke(action, parameters.map(_.apply(ctx)): _*)
    outputMapper(result)
  }

}

private class ServerStreamedCallInvoker(protected val method: Method,
                                        protected val serviceMethod: ResolvedServiceMethod[_, _],
                                        protected val anySupport: AnySupport)
    extends UnaryInSupport
    with StreamedOutSupport {

  def invoke(action: AnyRef,
             message: MessageEnvelope[JavaPbAny],
             context: ActionContext): javadsl.Source[Reply[JavaPbAny], NotUsed] = {
    val ctx = InvocationContext(message, context)
    val result = method.invoke(action, parameters.map(_.apply(ctx)): _*)
    outputMapper(result)
  }

}

private class ClientStreamedCallInvoker(protected val method: Method,
                                        protected val serviceMethod: ResolvedServiceMethod[_, _],
                                        protected val materializer: Materializer,
                                        protected val anySupport: AnySupport)
    extends UnaryOutSupport
    with StreamedInSupport {

  def invoke(action: AnyRef,
             stream: javadsl.Source[MessageEnvelope[JavaPbAny], NotUsed],
             context: ActionContext): CompletionStage[Reply[JavaPbAny]] = {
    val ctx = InvocationContext(stream, context)
    val result = method.invoke(action, parameters.map(_.apply(ctx)): _*)
    outputMapper(result)
  }

}

private class StreamedCallInvoker(protected val method: Method,
                                  protected val serviceMethod: ResolvedServiceMethod[_, _],
                                  protected val materializer: Materializer,
                                  protected val anySupport: AnySupport)
    extends StreamedOutSupport
    with StreamedInSupport {

  def invoke(action: AnyRef,
             stream: javadsl.Source[MessageEnvelope[JavaPbAny], NotUsed],
             context: ActionContext): javadsl.Source[Reply[JavaPbAny], NotUsed] = {
    val ctx = InvocationContext(stream, context)
    val result = method.invoke(action, parameters.map(_.apply(ctx)): _*)
    outputMapper(result)
  }

}
