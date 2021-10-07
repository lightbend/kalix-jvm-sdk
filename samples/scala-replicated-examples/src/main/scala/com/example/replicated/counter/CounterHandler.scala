package com.example.replicated.counter

import com.akkaserverless.replicatedentity.ReplicatedData
import com.akkaserverless.scalasdk.impl.replicatedentity.ReplicatedEntityHandler
import com.akkaserverless.scalasdk.replicatedentity.CommandContext
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedCounter
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity

class CounterHandler(entity: Counter) extends ReplicatedEntityHandler[ReplicatedCounter, Counter](entity) {

  override def handleCommand(
      commandName: String,
      data: ReplicatedData,
      command: Any,
      context: CommandContext): ReplicatedEntity.Effect[_] = {

    val scalaData = data.asInstanceOf[ReplicatedCounter]
    commandName match {
      case "Increase" =>
        entity.increase(scalaData, command.asInstanceOf[IncreaseValue])
      case "Decrease" =>
        entity.decrease(scalaData, command.asInstanceOf[DecreaseValue])
      case "Get" =>
        entity.get(scalaData, command.asInstanceOf[GetValue])
    }
  }
}
