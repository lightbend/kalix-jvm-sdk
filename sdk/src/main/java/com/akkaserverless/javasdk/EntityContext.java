/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

/**
 * Root context for all contexts that pertain to entities, that is, things that are addressable via
 * an entity id.
 */
public interface EntityContext extends Context {

  /**
   * The id of the entity that this context is for.
   *
   * @return The entity id.
   */
  String entityId();
}
