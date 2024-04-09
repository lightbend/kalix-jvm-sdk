/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.replicatedentity;

import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter;
import kalix.javasdk.replicatedentity.CommandContext;
import kalix.javasdk.replicatedentity.ReplicatedCounter;
import kalix.javasdk.replicatedentity.ReplicatedEntity;
import kalix.tck.model.ReplicatedEntity.Request;

public class ReplicatedEntityTwoEntityRouter
    extends ReplicatedEntityRouter<ReplicatedCounter, ReplicatedEntityTwoEntity> {

  public ReplicatedEntityTwoEntityRouter(ReplicatedEntityTwoEntity entity) {
    super(entity);
  }

  @Override
  public ReplicatedEntity.Effect<?> handleCommand(
      String commandName, ReplicatedCounter data, Object command, CommandContext context) {
    switch (commandName) {
      case "Call":
        return entity().call(data, (Request) command);
      default:
        throw new CommandHandlerNotFound(commandName);
    }
  }
}
