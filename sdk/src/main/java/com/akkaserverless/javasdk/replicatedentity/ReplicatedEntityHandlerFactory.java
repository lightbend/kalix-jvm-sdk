/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.replicatedentity;

/**
 * Low level interface for handling commands for replicated entities.
 *
 * <p>Generally, this should not be used, rather, a {@link ReplicatedEntity} annotated class should
 * be used.
 */
public interface ReplicatedEntityHandlerFactory {
  /**
   * Create a Replicated Entity handler for the given context.
   *
   * <p>This will be invoked each time a new replicated entity stream from the proxy is established,
   * for handling commands for a single Replicated Entity.
   *
   * @param context The creation context.
   * @return The handler to handle commands.
   */
  ReplicatedEntityHandler create(ReplicatedEntityCreationContext context);
}
