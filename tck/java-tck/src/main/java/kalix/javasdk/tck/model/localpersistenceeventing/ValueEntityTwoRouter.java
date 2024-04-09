/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.impl.valueentity.ValueEntityRouter;
import kalix.javasdk.valueentity.CommandContext;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.tck.model.eventing.LocalPersistenceEventing;

/** A value entity handler */
public class ValueEntityTwoRouter extends ValueEntityRouter<Object, ValueEntityTwo> {

  public ValueEntityTwoRouter(ValueEntityTwo entity) {
    super(entity);
  }

  @Override
  public ValueEntity.Effect<?> handleCommand(
      String commandName, Object state, Object command, CommandContext context) {
    switch (commandName) {
      case "UpdateJsonValue":
        return entity().updateJsonValue(state, (LocalPersistenceEventing.JsonValue) command);
      default:
        throw new CommandHandlerNotFound(commandName);
    }
  }
}
