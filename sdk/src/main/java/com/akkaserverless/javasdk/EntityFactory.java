/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

/** Entity factory for supporting DI environments. */
public interface EntityFactory {
  /**
   * Create an entity.
   *
   * @return the new entity
   */
  Object create(EntityContext context);

  /**
   * Get the class of the entity.
   *
   * @return the entity class
   */
  Class<?> entityClass();
}
