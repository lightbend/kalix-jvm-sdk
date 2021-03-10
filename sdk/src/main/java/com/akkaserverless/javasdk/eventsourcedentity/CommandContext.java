/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.eventsourcedentity;

import com.akkaserverless.javasdk.ClientActionContext;
import com.akkaserverless.javasdk.EffectContext;
import com.akkaserverless.javasdk.MetadataContext;

/**
 * An event sourced command context.
 *
 * <p>Methods annotated with {@link CommandHandler} may take this is a parameter. It allows emitting
 * new events in response to a command, along with forwarding the result to other entities, and
 * performing side effects on other entities.
 */
public interface CommandContext
    extends EventSourcedContext, ClientActionContext, EffectContext, MetadataContext {
  /**
   * The current sequence number of events in this entity.
   *
   * @return The current sequence number.
   */
  long sequenceNumber();

  /**
   * The name of the command being executed.
   *
   * @return The name of the command.
   */
  String commandName();

  /**
   * The id of the command being executed.
   *
   * @return The id of the command.
   */
  long commandId();

  /**
   * Emit the given event. The event will be persisted, and the handler of the event defined in the
   * current behavior will immediately be executed to pick it up.
   *
   * @param event The event to emit.
   */
  void emit(Object event);
}
