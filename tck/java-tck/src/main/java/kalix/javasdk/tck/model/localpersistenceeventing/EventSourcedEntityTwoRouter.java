/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.JsonSupport;
import kalix.javasdk.eventsourcedentity.CommandContext;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Any;

/** An event sourced entity handler */
public class EventSourcedEntityTwoRouter
    extends EventSourcedEntityRouter<String, Object, EventSourcedEntityTwo> {

  public EventSourcedEntityTwoRouter(EventSourcedEntityTwo entity) {
    super(entity);
  }

  @Override
  public String handleEvent(String state, Object event) {
    // FIXME requirement to use JSON events should be removed from TCK
    if (event instanceof Any) {
      return entity().handle(state, JsonSupport.decodeJson(JsonMessage.class, (Any) event));
    } else {
      throw new EventSourcedEntityRouter.EventHandlerNotFound(event.getClass());
    }
  }

  @Override
  public EventSourcedEntity.Effect<?> handleCommand(
      String commandName, String state, Object command, CommandContext context) {
    switch (commandName) {
      case "EmitJsonEvent":
        return entity().emitJsonEvent(state, (LocalPersistenceEventing.JsonEvent) command);

      default:
        throw new EventSourcedEntityRouter.CommandHandlerNotFound(commandName);
    }
  }
}
