package com.example.replicated.multimap

import com.example.replicated.multimap
import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter.CommandHandlerNotFound
import kalix.scalasdk.impl.replicatedentity.ReplicatedEntityRouter
import kalix.scalasdk.replicatedentity.CommandContext
import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedMultiMap

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A replicated entity handler that is the glue between the Protobuf service `MultiMapService`
 * and the command handler methods in the `SomeMultiMap` class.
 */
class SomeMultiMapRouter(entity: SomeMultiMap)
  extends ReplicatedEntityRouter[ReplicatedMultiMap[SomeKey, SomeValue], SomeMultiMap](entity) {

  override def handleCommand(
      commandName: String,
      data: ReplicatedMultiMap[SomeKey, SomeValue],
      command: Any,
      context: CommandContext): ReplicatedEntity.Effect[_] = {

    commandName match {
      case "Put" =>
        entity.put(data, command.asInstanceOf[PutValue])

      case _ =>
        throw new CommandHandlerNotFound(commandName)
    }
  }
}
