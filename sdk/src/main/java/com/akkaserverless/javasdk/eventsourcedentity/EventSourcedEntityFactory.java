/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.eventsourcedentity;

/**
 * Low level interface for handling events and commands on an entity.
 *
 * <p>Generally, this should not be needed, instead, a class annotated with the {@link
 * com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity @EventSourcedEntity} and similar
 * annotations should be used.
 */
public interface EventSourcedEntityFactory {
  /**
   * Create an entity handler for the given context.
   *
   * @param context The context.
   * @return The handler for the given context.
   */
  EventSourcedEntityHandler create(EventSourcedContext context);
}
