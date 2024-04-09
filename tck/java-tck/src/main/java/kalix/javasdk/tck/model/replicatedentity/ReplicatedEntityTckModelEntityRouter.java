/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.replicatedentity;

import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter;
import kalix.javasdk.replicatedentity.CommandContext;
import kalix.replicatedentity.ReplicatedData;
import kalix.javasdk.replicatedentity.ReplicatedEntity;
import kalix.tck.model.ReplicatedEntity.Request;

public class ReplicatedEntityTckModelEntityRouter
    extends ReplicatedEntityRouter<ReplicatedData, ReplicatedEntityTckModelEntity> {

  public ReplicatedEntityTckModelEntityRouter(ReplicatedEntityTckModelEntity entity) {
    super(entity);
  }

  @Override
  public ReplicatedEntity.Effect<?> handleCommand(
      String commandName, ReplicatedData data, Object command, CommandContext context) {
    switch (commandName) {
      case "Process":
        return entity().process(data, (Request) command);
      default:
        throw new CommandHandlerNotFound(commandName);
    }
  }
}
