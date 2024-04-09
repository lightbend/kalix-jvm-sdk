/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.eventsourcedentity.CommandContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;
import kalix.tck.model.eventing.LocalPersistenceEventing;

/** An event sourced entity handler */
public class EventSourcedEntityOneRouter
    extends EventSourcedEntityRouter<String, Object, EventSourcedEntityOne> {

  public EventSourcedEntityOneRouter(EventSourcedEntityOne entity) {
    super(entity);
  }

  @Override
  public String handleEvent(String state, Object event) {
    if (event instanceof LocalPersistenceEventing.EventOne) {
      return entity().handle(state, (LocalPersistenceEventing.EventOne) event);
    } else if (event instanceof LocalPersistenceEventing.EventTwo) {
      return entity().handle(state, (LocalPersistenceEventing.EventTwo) event);
    } else {
      throw new EventSourcedEntityRouter.EventHandlerNotFound(event.getClass());
    }
  }

  @Override
  public EventSourcedEntity.Effect<?> handleCommand(
      String commandName, String state, Object command, CommandContext context) {
    switch (commandName) {
      case "EmitEvent":
        return entity().emitEvent(state, (LocalPersistenceEventing.EmitEventRequest) command);

      default:
        throw new EventSourcedEntityRouter.CommandHandlerNotFound(commandName);
    }
  }
}
