/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.eventsourcedentity

import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.eventsourcedentity.CommandContext
import kalix.javasdk.eventsourcedentity.EventSourcedEntity
import kalix.javasdk.impl.CommandHandler
import kalix.javasdk.impl.InvocationContext
import kalix.javasdk.impl.JsonMessageCodec
import kalix.javasdk.impl.MethodInvoker

import java.lang.reflect.ParameterizedType

class ReflectiveEventSourcedEntityRouter[S, E, ES <: EventSourcedEntity[S, E]](
    override protected val entity: ES,
    commandHandlers: Map[String, CommandHandler],
    eventHandlerMethods: Map[String, MethodInvoker],
    messageCodec: JsonMessageCodec)
    extends EventSourcedEntityRouter[S, E, ES](entity) {

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(
      commandName,
      throw new HandlerNotFoundException("command", commandName, commandHandlers.keySet))

  private def eventHandlerLookup(eventName: String) = {
    eventHandlerMethods.getOrElse(
      messageCodec.removeVersion(eventName),
      throw new HandlerNotFoundException("event", eventName, eventHandlerMethods.keySet))
  }

  override def handleEvent(state: S, event: E): S = {

    _extractAndSetCurrentState(state)

    event match {
      case s: ScalaPbAny => // replaying event coming from proxy
        val invocationContext = InvocationContext(s, JavaPbAny.getDescriptor)

        eventHandlerLookup(s.typeUrl)
          .invoke(entity, invocationContext)
          .asInstanceOf[S]

      case _ => // processing runtime event coming from memory
        val typeName = messageCodec.typeUrlFor(event.getClass)

        eventHandlerLookup(typeName).method
          .invoke(entity, event.asInstanceOf[event.type])
          .asInstanceOf[S]
    }
  }

  override def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      commandContext: CommandContext): EventSourcedEntity.Effect[_] = {

    _extractAndSetCurrentState(state)

    val commandHandler = commandHandlerLookup(commandName)
    val invocationContext =
      InvocationContext(
        command.asInstanceOf[ScalaPbAny],
        commandHandler.requestMessageDescriptor,
        commandContext.metadata())

    val inputTypeUrl = command.asInstanceOf[ScalaPbAny].typeUrl
    val methodInvoker = commandHandler
      .getInvoker(inputTypeUrl)

    methodInvoker
      .invoke(entity, invocationContext)
      .asInstanceOf[EventSourcedEntity.Effect[_]]
  }

  private def _extractAndSetCurrentState(state: S): Unit = {
    val entityStateType: Class[S] =
      this.entity.getClass.getGenericSuperclass
        .asInstanceOf[ParameterizedType]
        .getActualTypeArguments
        .head
        .asInstanceOf[Class[S]]

    // the state: S received can either be of the entity "state" type (if coming from emptyState/memory)
    // or PB Any type (if coming from the proxy)
    state match {
      case s if s == null || state.getClass == entityStateType =>
        // note that we set the state even if null, this is needed in order to
        // be able to call currentState() later
        entity._internalSetCurrentState(s)
      case s =>
        val deserializedState =
          JsonSupport.decodeJson(entityStateType, ScalaPbAny.toJavaProto(s.asInstanceOf[ScalaPbAny]))
        entity._internalSetCurrentState(deserializedState)
    }
  }
}

final class HandlerNotFoundException(handlerType: String, name: String, availableHandlers: Set[String])
    extends RuntimeException(
      s"no matching $handlerType handler for '$name'. " +
      s"Available handlers are: [${availableHandlers.mkString(", ")}]")
