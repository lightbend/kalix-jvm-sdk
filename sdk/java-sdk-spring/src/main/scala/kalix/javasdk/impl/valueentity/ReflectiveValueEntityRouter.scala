/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.valueentity

import java.lang.reflect.ParameterizedType

import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.CommandHandler
import kalix.javasdk.impl.InvocationContext
import kalix.javasdk.valueentity.CommandContext
import kalix.javasdk.valueentity.ValueEntity

class ReflectiveValueEntityRouter[S, E <: ValueEntity[S]](
    override protected val entity: E,
    commandHandlers: Map[String, CommandHandler])
    extends ValueEntityRouter[S, E](entity) {

  private def commandHandlerLookup(commandName: String) =
    commandHandlers.getOrElse(commandName, throw new RuntimeException(s"no matching method for '$commandName'"))

  override protected def handleCommand(
      commandName: String,
      state: S,
      command: Any,
      commandContext: CommandContext): ValueEntity.Effect[_] = {

    _extractAndSetCurrentState(state)

    val commandHandler = commandHandlerLookup(commandName)
    val invocationContext =
      InvocationContext(
        command.asInstanceOf[ScalaPbAny],
        commandHandler.requestMessageDescriptor,
        commandContext.metadata())

    val inputTypeUrl = command.asInstanceOf[ScalaPbAny].typeUrl

    commandHandler
      .getInvoker(inputTypeUrl)
      .invoke(entity, invocationContext)
      .asInstanceOf[ValueEntity.Effect[_]]
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
