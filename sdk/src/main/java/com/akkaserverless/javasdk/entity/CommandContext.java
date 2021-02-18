/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.entity;

import com.akkaserverless.javasdk.ClientActionContext;
import com.akkaserverless.javasdk.EffectContext;
import com.akkaserverless.javasdk.MetadataContext;

import java.util.Optional;

/**
 * A value based entity command context.
 *
 * <p>Methods annotated with {@link CommandHandler} may take this is a parameter. It allows updating
 * or deleting the entity state in response to a command, along with forwarding the result to other
 * entities, and performing side effects on other entities.
 */
public interface CommandContext<T>
    extends EntityContext, ClientActionContext, EffectContext, MetadataContext {

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
   * Retrieve the state.
   *
   * @return the current state or empty if none have been created.
   * @throws IllegalStateException If the current entity state have been deleted in the command
   *     invocation.
   */
  Optional<T> getState();

  /**
   * Update the entity with the new state. The state will be persisted.
   *
   * @param state The state to persist.
   */
  void updateState(T state);

  /** Delete the entity state. */
  void deleteState();
}
