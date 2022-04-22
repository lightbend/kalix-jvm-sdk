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
 * and the command handler methods in the `MultiMapServiceEntity` class.
 */
class MultiMapServiceEntityRouter(entity: MultiMapServiceEntity)
  extends ReplicatedEntityRouter[ReplicatedMultiMap[com.example.replicated.multimap.domain.SomeKey, com.example.replicated.multimap.domain.SomeValue], MultiMapServiceEntity](entity) {

  override def handleCommand(
      commandName: String,
      data: ReplicatedMultiMap[com.example.replicated.multimap.domain.SomeKey, com.example.replicated.multimap.domain.SomeValue],
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
