/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.action

import akka.NotUsed
import akka.stream.javadsl.Source
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.action.Action
import kalix.javasdk.action.MessageEnvelope
import kalix.javasdk.impl.AnySupport.ProtobufEmptyTypeUrl
import kalix.javasdk.impl.CommandHandler
import kalix.javasdk.impl.InvocationContext
import kalix.javasdk.impl.reflection.Reflect

// TODO: abstract away reactor dependency
import reactor.core.publisher.Flux

class ReflectiveActionRouter[A <: Action](
    action: A,
    commandHandlers: Map[String, CommandHandler],
    ignoreUnknown: Boolean)
    extends ActionRouter[A](action) {

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[_] = {

    val commandHandler = commandHandlerLookup(commandName)

    val invocationContext =
      InvocationContext(
        message.payload().asInstanceOf[ScalaPbAny],
        commandHandler.requestMessageDescriptor,
        message.metadata())

    val inputTypeUrl = message.payload().asInstanceOf[ScalaPbAny].typeUrl
    val methodInvoker = commandHandler.lookupInvoker(inputTypeUrl)

    // lookup ComponentClient
    val componentClients = Reflect.lookupComponentClientFields(action)

    try {
      componentClients.foreach(_.setCallMetadata(message.metadata()))

      methodInvoker match {
        case Some(invoker) =>
          inputTypeUrl match {
            case ProtobufEmptyTypeUrl =>
              invoker
                .invoke(action)
                .asInstanceOf[Action.Effect[_]]
            case _ =>
              invoker
                .invoke(action, invocationContext)
                .asInstanceOf[Action.Effect[_]]
          }
        case None if ignoreUnknown => ActionEffectImpl.Builder.ignore()
        case None =>
          throw new NoSuchElementException(
            s"Couldn't find any method with input type [$inputTypeUrl] in Action [$action].")
      }
    } finally {
      componentClients.foreach(_.clearCallMetadata())
    }
  }

  override def handleStreamedOut(
      commandName: String,
      message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = {

    val componentMethod = commandHandlerLookup(commandName)

    // lookup ComponentClient
    val componentClients = Reflect.lookupComponentClientFields(action)

    try {
      componentClients.foreach(_.setCallMetadata(message.metadata()))
      val context =
        InvocationContext(
          message.payload().asInstanceOf[ScalaPbAny],
          componentMethod.requestMessageDescriptor,
          message.metadata())

      val inputTypeUrl = message.payload().asInstanceOf[ScalaPbAny].typeUrl
      componentMethod.lookupInvoker(inputTypeUrl) match {
        case Some(methodInvoker) =>
          val response = methodInvoker.invoke(action, context).asInstanceOf[Flux[Action.Effect[_]]]
          Source.fromPublisher(response)
        case None if ignoreUnknown => Source.empty()
        case None =>
          throw new NoSuchElementException(
            s"Couldn't find any method with input type [$inputTypeUrl] in Action [$action].")
      }
    } finally {
      componentClients.foreach(_.clearCallMetadata())
    }
  }

  override def handleStreamedIn(commandName: String, stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[_] =
    throw new IllegalArgumentException("Stream in calls are not supported")

  // TODO: to implement
  override def handleStreamed(
      commandName: String,
      stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] =
    throw new IllegalArgumentException("Stream in calls are not supported")
}
